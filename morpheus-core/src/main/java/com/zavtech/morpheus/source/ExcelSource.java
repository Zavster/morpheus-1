package com.zavtech.morpheus.source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.parsers.ParserConfigurationException;

import com.univocity.parsers.common.ParserOutput;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.SAXHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import static java.util.Arrays.stream;

import com.zavtech.morpheus.array.Array;
import com.zavtech.morpheus.frame.DataFrame;
import com.zavtech.morpheus.frame.DataFrameException;
import com.zavtech.morpheus.frame.DataFrameSource;
import com.zavtech.morpheus.index.Index;
import com.zavtech.morpheus.util.Resource;
import com.zavtech.morpheus.util.text.Formats;
import com.zavtech.morpheus.util.text.parser.Parser;

/**
 * A DataFrameSource designed to load DataFrames from Microsoft Excel.
 *
 * @param <R>   the row key type
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 *
 * @author  Dwight Gunning
 */
public class ExcelSource<R> extends DataFrameSource<R,String,ExcelSourceOptions<R>> {

    public ExcelSource(){super();}

    /**
     * Read from the excel resource
     * @param configurator  the options consumer to configure load options
     * @return a new DataFrame
     * @throws DataFrameException if the DataFrame cannot be constructed
     */
    @Override
    public DataFrame<R, String> read(Consumer<ExcelSourceOptions<R>> configurator) throws DataFrameException {
        final ExcelSourceOptions<R> options = initOptions(new ExcelSourceOptions<>(), configurator);
        switch (options.getExcelType()){
            case XLSX: return readXslx(options);
            case XLS: return readXls(options);
        }
        throw new DataFrameException("Unimplemented excel type");
    }

    private DataFrame<R,String> readXls(ExcelSourceOptions<R> options) {
        final Resource resource = options.getResource();
        try {
            switch (resource.getType()) {
                case FILE: return readWorkbook(options, WorkbookFactory.create(resource.asFile()));
                case URL: return readWorkbook(options, WorkbookFactory.create(new File(resource.asURL().getFile())));
                case INPUT_STREAM: return readWorkbook(options, WorkbookFactory.create(resource.asInputStream()));
                default: throw new DataFrameException("Unsupported resource specified in ExcelRequest: " + resource);
            }
        }catch (IOException | InvalidFormatException e){
            throw new DataFrameException("Cannot read excel file: " + resource, e);
        }
    }

    /**
     * Read the workbook and return a DataFrame
     * @param options the ExcelSourceOptions
     * @param workbook the workbook to read
     * @return a new DataFrame
     * @throws IOException
     */
    private DataFrame<R,String> readWorkbook(ExcelSourceOptions<R> options, Workbook workbook) throws IOException{
        try {
            final Sheet sheet = getSheetForParsing(options, workbook);
            XlsParser parser = new XlsParser(options);
            return parser.parse(sheet);
        }finally{
            workbook.close();
        }
    }


    private DataFrame<R, String> readXslx(ExcelSourceOptions<R> options) throws DataFrameException {
        final Resource resource = options.getResource();
        try(OPCPackage opcPackage = open(resource)){
            return parse(options, opcPackage);
        } catch (DataFrameException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DataFrameException("Failed to create DataFrame from Excel source", ex);
        }
    }

    /**
     * Opens the excel resource
     * @param resource the resource to open
     * @return an OPCPackage representing the excel resource
     * @throws IOException if an io problem occurs
     * @throws InvalidFormatException if the file has an incorrect format
     */
    private OPCPackage open(Resource resource) throws IOException, InvalidFormatException {
        switch (resource.getType()) {
            case FILE: return OPCPackage.open(resource.asFile());
            case URL: return OPCPackage.open(resource.asURL().getPath());
            case INPUT_STREAM: return OPCPackage.open(resource.asInputStream());
            default: throw new DataFrameException("Unsupported resource specified in ExcelRequest: " + resource);
        }
    }

    /**
     * Returns a DataFrame parsed from the stream specified stream
     *
     * @return the DataFrame parsed from stream
     * @throws IOException if there stream read error
     */
    private DataFrame<R, String> parse(ExcelSourceOptions<R> options, OPCPackage opcPackage) throws IOException {
        try {
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(opcPackage);
            XSSFReader xssfReader = new XSSFReader(opcPackage);
            XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();

            try (InputStream inputStream = getSheetForParsing( options, iter)) {

                StylesTable styles = xssfReader.getStylesTable();
                XMLReader sheetParser = SAXHelper.newXMLReader();
                final ExcelXSSFSheetContentHandler sheetContentsHandler = new ExcelXSSFSheetContentHandler(options);
                DataFormatter formatter = new DataFormatter();

                ContentHandler contentHandler = new XSSFSheetXMLHandler(styles, null, strings, sheetContentsHandler, formatter, false);
                sheetParser.setContentHandler(contentHandler);
                sheetParser.parse(new InputSource(inputStream));
                sheetContentsHandler.endProcess();

                return sheetContentsHandler.getFrame();
            } catch (InvalidFormatException e) {
                throw new DataFrameException("Failed to parse Excel file - invalid format", e);
            } catch (ParserConfigurationException e) {
                throw new DataFrameException("Failed to parse Excel file - parser configuration issue", e);
            }
        } catch (OpenXML4JException|SAXException ex) {
            throw new DataFrameException("Failed to parse Excel file", ex);
        }
    }

    /**
     * Get the sheet that should be parsed
     * @param options The ExcelSourceOptions
     * @param workbook The Excel workbook (OLE)
     * @return A sheet that matches the name if the sheet name is provided
     */
    private Sheet getSheetForParsing(ExcelSourceOptions<R> options, Workbook workbook) {
        Sheet theSheet = options.getSheetName() != null?
                workbook.getSheet(options.getSheetName())
                : workbook.getSheetAt(0);
        if( theSheet != null){
            return theSheet;
        }
        throw new DataFrameException("No sheet found for that matched configured sheet " + options.getSheetName());
    }

    /**
     * Get the sheet that should be parsed
     * @param options The options
     * @param iter The sheet iterator
     * @return A sheet that matches the name if the sheet name is provided
     */
    private InputStream getSheetForParsing(ExcelSourceOptions<R> options, XSSFReader.SheetIterator iter) {
        while(iter.hasNext()){
            InputStream inputStream = iter.next();
            final String sheetName = iter.getSheetName();
            if(options.getSheetName() == null){
                return inputStream;
            }else{
                if( sheetName.equals(options.getSheetName())){
                    return inputStream;
                }
            }
        }
        throw new DataFrameException("No sheet found for that matched configured sheet " + options.getSheetName());
    }




    /**
     * Handles the excel parsing events and populates the DataFrame
     *
     * This handles only the newer Excel XSLX format.
     */
    class ExcelSheetContentHandler implements Runnable{

        int rowCounter;
        String[] headers;
        int[] colIndexes;
        String[] rowValues;
        final int logBatchSize;
        volatile boolean done;
        protected ParserOutput output;
        final Function<String[],R> rowKeyParser;
        CsvSource.DataBatch<R> batch;
        final ExcelSourceOptions<R> options;
        DataFrame<R,String> frame;
        Parser<?>[] parsers;
        CountDownLatch countDownLatch;
        final Predicate<String[]> rowPredicate;
        LinkedTransferQueue<CsvSource.DataBatch<R>> queue;
        final Object lock = new Object();

        ExcelSheetContentHandler(ExcelSourceOptions<R> options){
            this.options = options;
            this.rowPredicate = options.getRowPredicate().orElse(null);
            this.rowKeyParser = options.getRowKeyParser().orElse(null);
            this.logBatchSize = options.getLogBatchSize();
            this.output = new ParserOutput(new CsvParserSettings());
            if (options.isParallel()) {
                this.countDownLatch = new CountDownLatch(1);
                this.queue = new LinkedTransferQueue<>();
                final Thread thread = new Thread(this, "DataFrameExcelReaderThread");
                thread.setDaemon(true);
                thread.start();
            }
        }

        /**
         * Returns true if processing is complete
         * @return  true if processing is complete
         */
        boolean isComplete() {
            synchronized (lock) {
                return done && queue.isEmpty();
            }
        }

        @Override()
        public void run() {
            try {
                while (!isComplete()) {
                    try {
                        final CsvSource.DataBatch<R> batch = queue.take();
                        if (batch != null && batch.rowCount() > 0) {
                            processBatch(batch);
                        }
                    } catch (Exception ex) {
                        throw new DataFrameException("Failed to process CSV data batch", ex);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                countDownLatch.countDown();
            }
        }

        public DataFrame<R,String> getFrame() {
            try {
                if(options.isParallel()){
                    countDownLatch.await();
                }
                return frame;
            } catch (Exception ex) {
                throw new DataFrameException("Failed to resolve frame", ex);
            }
        }

        /**
         * Initializes data structures to capture parsed content
         * @param csvColCount   the number of columns in CSV content
         */
        @SuppressWarnings("unchecked")
        protected void initBatch(int csvColCount, String[] headers) {
            initHeader(csvColCount, headers);
            this.rowValues = new String[csvColCount];
            this.batch = new CsvSource.DataBatch(options.getRowAxisType(), options.getReadBatchSize(), colIndexes.length);
            this.parsers = new Parser[csvColCount];
        }

        /**
         * Initializes the header array and column ordinals
         * @param csvColCount   the number of columns in CSV content
         * @return              the column count for frame
         */
        protected int initHeader(int csvColCount, String[] headers) {
            this.headers = options.isHeader() ? headers : IntStream.range(0, csvColCount).mapToObj(i -> "Column-" + i).toArray(String[]::new);
            this.headers = IntStream.range(0, headers.length).mapToObj(i -> headers[i] != null ? headers[i] : "Column-" + i).toArray(String[]::new);
            this.colIndexes = IntStream.range(0, headers.length).toArray();
            this.options.getColIndexPredicate().ifPresent(predicate -> {
                final Map<String,Integer> indexMap = IntStream.range(0, headers.length).boxed().collect(Collectors.toMap(i -> headers[i], i -> colIndexes[i]));
                this.headers = stream(headers).filter(colName -> predicate.test(indexMap.get(colName))).toArray(String[]::new);
                this.colIndexes = stream(headers).mapToInt(indexMap::get).toArray();
            });
            this.options.getColNamePredicate().ifPresent(predicate -> {
                final Map<String,Integer> indexMap = IntStream.range(0, headers.length).boxed().collect(Collectors.toMap(i -> headers[i], i -> colIndexes[i]));
                this.headers = stream(headers).filter(predicate).toArray(String[]::new);
                this.colIndexes = stream(headers).mapToInt(indexMap::get).toArray();
            });
            this.options.getColumnNameMapping().ifPresent(mapping -> {
                final IntStream colOrdinals = IntStream.range(0, headers.length);
                this.headers = colOrdinals.mapToObj(ordinal -> mapping.apply(headers[ordinal], ordinal)).toArray(String[]::new);
            });
            return colIndexes.length;
        }

        /**
         * Initializes the frame based on the contents of the first batch
         * @param batch     the initial batch to initialize frame
         */
        protected void initFrame(CsvSource.DataBatch<R> batch) {
            if (headers == null) {
                final Class<R> rowType = options.getRowAxisType();
                final Index<R> rowKeys = Index.of(rowType, 1);
                final Index<String> colKeys = Index.of(String.class, 1);
                this.frame = DataFrame.of(rowKeys, colKeys, Object.class);
            } else {
                final int colCount = headers.length;
                final Formats formats = options.getFormats();
                final Class<R> rowType = options.getRowAxisType();
                final Index<R> rowKeys = Index.of(rowType, options.getRowCapacity().orElse(10000));
                final Index<String> colKeys = Index.of(String.class, colCount);
                this.frame = DataFrame.of(rowKeys, colKeys, Object.class);
                for (int i=0; i<colCount; ++i) {
                    final String colName = headers[i] != null ? headers[i] : "Column-" + i;
                    try {
                        final String[] rawValues = batch.colData(i);
                        final Optional<Parser<?>> userParser = CsvSource.getParser(options.getFormats(), colName);
                        final Optional<Class<?>> colType = getColumnType(colName);
                        if (colType.isPresent()) {
                            final Class<?> type = colType.get();
                            final Parser<?> parser = userParser.orElse(formats.getParserOrFail(colType.get(), Object.class));
                            this.parsers[i] = parser;
                            this.frame.cols().add(colName, type);
                        } else {
                            final Parser<?> stringParser = formats.getParserOrFail(String.class);
                            final Parser<?> parser = userParser.orElse(formats.findParser(rawValues).orElse(stringParser));
                            final Set<Class<?>> typeSet = stream(rawValues).map(parser).filter(Objects::nonNull).map(Object::getClass).collect(Collectors.toSet());
                            final Class<?> type = typeSet.size() == 1 ? typeSet.iterator().next() : Object.class;
                            this.parsers[i] = parser;
                            this.frame.cols().add(colName, type);
                        }
                    } catch (Exception ex) {
                        throw new DataFrameException("Failed to inspect seed values in column: " + colName, ex);
                    }
                }
            }
        }

        /**
         * Returns the column type for the column name
         * @param colName   the column name
         * @return          the column type
         */
        Optional<Class<?>> getColumnType(String colName) {
            final Optional<Class<?>> colType = options.getColumnType(colName);
            if (colType.isPresent()) {
                return colType;
            } else {
                for (Map.Entry<String,Class<?>> entry : options.getColTypeMap().entrySet()) {
                    if (colName.matches( entry.getKey())) {
                        return Optional.of(entry.getValue());
                    }
                }
                return Optional.empty();
            }
        }

        /**
         * Processes the batch of data provided
         * @param batch the batch reference
         */
        protected void processBatch(CsvSource.DataBatch<R> batch) {
            int rowIndex = -1;
            try {
                if (frame == null) {
                    initFrame(batch);
                }
                if (batch.rowCount() > 0) {
                    final Array<R> keys = batch.keys();
                    final int rowCount = batch.rowCount();
                    final int fromRowIndex = frame.rowCount();
                    final Array<R> rowKeys = rowCount < options.getReadBatchSize() ? keys.copy(0, rowCount) : keys;
                    this.frame.rows().addAll(rowKeys);
                    for (int j=0; j<colIndexes.length; ++j) {
                        final String[] colValues = batch.colData(j);
                        final Parser<?> parser = parsers[j];
                        for (int i=0; i<rowCount; ++i) {
                            rowIndex = fromRowIndex + i;
                            final String rawValue = colValues[i];
                            switch (parser.getStyle()) {
                                case INTEGER:   frame.setIntAt(rowIndex, j, parser.applyAsInt(rawValue));          break;
                                case BOOLEAN:   frame.setBooleanAt(rowIndex, j, parser.applyAsBoolean(rawValue));  break;
                                case LONG:      frame.setLongAt(rowIndex, j, parser.applyAsLong(rawValue));        break;
                                case DOUBLE:    frame.setDoubleAt(rowIndex, j, parser.applyAsDouble(rawValue));    break;
                                default:        frame.setValueAt(rowIndex, j, parser.apply(rawValue));             break;
                            }
                        }
                    }
                    if (frame.rowCount() % 100000 == 0) {
                        System.out.println("Processed " + frame.rowCount() + " rows...");
                    }
                }
            } catch (Exception ex) {
                final int lineNo = options.isHeader() ? rowIndex + 2 : rowIndex + 1;
                throw new DataFrameException("Failed to process CSV batch, line no " + lineNo, ex);
            }
        }

        protected void endRow(String[] row, int rowIndex){
            try {
                if (batch == null) {
                    if(headers == null){
                        headers = row;
                    }
                    initBatch(row.length, headers);
                }

                if (rowIndex > 0 && (rowPredicate == null || rowPredicate.test(row)) ) {
                    this.rowCounter++;
                    if (logBatchSize > 0 && rowCounter % logBatchSize == 0) {
                        System.out.println("Loaded " + rowCounter + " rows...");
                    }
                    for (int i = 0; i < colIndexes.length; ++i) {
                        final int colIndex = colIndexes[i];
                        final String rawValue = row.length > colIndex ? row[colIndex] : null;
                        this.rowValues[i] = rawValue;
                    }
                    if (rowKeyParser == null) {
                        this.batch.addRow(rowCounter - 1, rowValues);
                    } else {
                        final R rowKey = rowKeyParser.apply(row);
                        this.batch.addRow(rowKey, rowValues);
                    }
                    if (batch.rowCount() == options.getReadBatchSize()) {
                        if (!options.isParallel()) {
                            this.processBatch(batch);
                            this.batch.clear();
                        } else {
                            synchronized (lock) {
                                this.queue.add(batch);
                                this.batch = new CsvSource.DataBatch<>(options.getRowAxisType(), options.getReadBatchSize(), colIndexes.length);
                                this.lock.notify();
                            }
                        }
                    }
                }
            } catch (DataFrameException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new DataFrameException("Failed to parse row: " + Arrays.toString(row), ex);
            }
        }

        void endProcess() {
            try {
                if (!options.isParallel()) {
                    this.batch = batch != null ? batch : new CsvSource.DataBatch<>(options.getRowAxisType(), options.getReadBatchSize(), 0);
                    this.processBatch(batch);
                } else {
                    synchronized (lock) {
                        this.done = true;
                        this.batch = batch != null ? batch : new CsvSource.DataBatch<>(options.getRowAxisType(), options.getReadBatchSize(), 0);
                        this.queue.add(batch);
                    }
                }
            } catch (DataFrameException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new DataFrameException("Failed to process CSV parse end", ex);
            }
        }
    }


    /**
     * A handler for Excel XML based data, implementing the XSSFSheetContentHandler interface
     *
     */
    class ExcelXSSFSheetContentHandler extends ExcelSheetContentHandler implements XSSFSheetXMLHandler.SheetContentsHandler{

        private int currentCol = -1;

        ExcelXSSFSheetContentHandler(ExcelSourceOptions options){
            super(options);
        }

        @Override
        public void startRow(int i) {
            if( this.output == null ){
                this.output = new ParserOutput(new CsvParserSettings());
            }
            currentCol = -1;
        }

        @Override
        public void endRow(int rowIndex) {
            String[] row = output.rowParsed();
            endRow(row, rowIndex);
        }

        /**
         * POI calls this with the value parsed from the cell.
         * @param cellReference The cell reference
         * @param formattedValue The value of the cell
         * @param comment a comment
         */
        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            this.output.valueParsed(formattedValue);
            int thisCol = (new CellReference(cellReference)).getCol();

            //Fill missing columns
            int missedCols = thisCol - currentCol - 1;
            for (int i=0; i<missedCols; i++) {
                this.output.valueParsed("");
            }
            currentCol = thisCol;
        }

        /**
         * Unused, and unimplemented. This comes from the from interface
         */
        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) { }
    }

    /**
     * Handles the OLE school xls parser
     */
    class XlsParser extends ExcelSheetContentHandler{


        XlsParser(ExcelSourceOptions<R> options) {
            super(options);
        }

        private DataFrame<R,String> parse(Sheet sheet) {
            DataFormatter formatter = new DataFormatter();
            int rowIndex = 0;
            for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext();){
                Row excelRow = rowIter.next();
                if( rowIndex ==0){
                    if( this.output == null ){
                        this.output = new ParserOutput(new CsvParserSettings());
                    }
                }
                final int numberOfColumns = excelRow.getLastCellNum();
                String[] row = new String[numberOfColumns];
                for(int i = 0; i < numberOfColumns; i++){
                    Cell cell = excelRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    row[i] = formatter.formatCellValue(cell);
                }
                endRow( row, rowIndex );
                rowIndex++;
            }
            endProcess();
            return this.frame;
        }
    }

}

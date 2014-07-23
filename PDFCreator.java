/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdflight;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.ElementHandler;
import com.itextpdf.tool.xml.Pipeline;
import com.itextpdf.tool.xml.Writable;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerFontProvider;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.tool.xml.exceptions.RuntimeWorkerException;
import com.itextpdf.tool.xml.html.CssAppliersImpl;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.WritableElement;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.ElementHandlerPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MediaTracker;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;

import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import pdflight.tests.FileSystemChecker;
import static java.nio.file.StandardCopyOption.*;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.JPanel;
import pdflight.tests.LoggerCheck;

/**
 *
 * @author Jakub Filla
 */
public class PDFCreator {

    static String imagesPath, templatesPath, outputsPath, fontsPath, basePath, s = "/", pdfName, pdf = ".pdf";
    static Document document;
    static Image templateImage;
    static PdfWriter writer;
    static PdfContentByte cb;
    static float compression;
    static float bleed;
    static XMLWorkerFontProvider fontProvider;
    static Config config;
    static Logger logger;
    static Logger globalLogger;
    static File lock;
    static final String CONFIG_FOLDER = "config";
    static Config globalConfig;
    static float height, width, widthBl, heightBl;
    static FileOutputStream fileStream;
    static ArrayList<Element> el = new ArrayList<>();
    static String defaultFont;
    static String projectDir;
    //ftp
    static FTPClient ftp;
    static String host;
    static String user;
    static String location;
    static String password;
    static Config ftpInfo;
    static Config loggerConfig;
    static boolean dryRunNoData = false;
    static boolean dryRun = false;
    static final String ER_NODOC = "unable to create pdf";
    static Set<String> notUsedFonts = new ConcurrentSkipListSet<>();
    static JSONArray data;
    static boolean checkFonts = false;
    static JSONArray response = new JSONArray();
    static JSONArray responseError = new JSONArray();
    static boolean enableUploadToFTP;
    static boolean enableMoveGeneratedFiles;
    static int deleteOldFilesAfter;

    static int pdfId;
    static int productId;
    static Config magicConfig;

    static boolean debug = false;
    static boolean jsonDump = false;
    //console

    public static void printHelp() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("help.txt"));
            String line = br.readLine();
            while (line != null) {
                System.out.println(line);
                line = br.readLine();
            }
        } catch (IOException ex) {
            System.err.println("help file not found");
            System.exit(1);
        }

    }

    public static void consoleRead(String[] args) {
        String[] allowed = {"help", "dryrun", "nodata", "checkconfig", "mkdir", "fonts", "lock", "rm", "lastrun", "log", "debug", "jsondump"};
        ParamParser pp = new ParamParser(args, allowed);
        debug = pp.has("debug");
        if (debug) {
            System.out.println("debug in on");
        }
        jsonDump = pp.has("jsondump");
        if (pp.has("help")) {
            printHelp();
            System.exit(0);
        }
        if (pp.has("dryrun")) {
            dryRun = true;
            if (pp.has("nodata")) {
                dryRunNoData = true;

            }
            return;
        }
        if (pp.has("lock")) {
            if (pp.has("rm")) {
                System.out.println("deleting .lock file");
                lock = new File(projectDir + s + ".lock");
                lock.delete();
                System.exit(0);
            } else {
                try {
                    System.out.println("creating .lock file");
                    lock = new File(projectDir + s + ".lock");
                    lock.createNewFile();
                    System.exit(0);
                } catch (IOException ex) {
                    System.out.println("unable to create lock file - " + ex);
                }
            }

        }
        if (pp.has("checkconfig")) {
            FileSystemChecker fsch;
            if (pp.getParams().size() == 1) {
                fsch = new FileSystemChecker(projectDir, pp.has("mkdir"), pp.getParams().get(0));
            } else {
                fsch = new FileSystemChecker(projectDir, pp.has("mkdir"));
            }
            if (fsch.run()) {
                System.exit(0);
            } else {
                System.exit(1);
            }
        }
        if (pp.has("lastrun")) {
            ObjectInputStream inObject = null;
            try {
                inObject = new ObjectInputStream(new FileInputStream(projectDir + s + "lastrun.time"));
                Date date = (Date) inObject.readObject();
                inObject.close();
                Date now = new Date();
                int minDelay = (int) ((now.getTime() - date.getTime()) / 60000);
                final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                System.out.println("last run at " + TIME_FORMAT.format(date) + " (" + minDelay + " min)");
                System.exit(0);
            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("unable to read " + projectDir + s + "lastrun.time -" + ex.getMessage());
                System.exit(1);
            }

        }
        if (pp.has("log")) {
            boolean ok = new LoggerCheck(projectDir + s + "config" + s + "logger.yml").run();
            if (ok) {
                System.exit(0);
            } else {
                System.exit(1);
            }

        }
        if (pp.has("fonts")) {
            checkFonts = true;
            System.out.println("generating test fonts pdf");
        }

    }
    //console end

    //init
    public static void fillProjectDir() {
        if (new BaseClass().runningFromJar()) {
            File f = new File(System.getProperty("java.class.path"));
            File dir = f.getAbsoluteFile().getParentFile();
            projectDir = dir.toString();
        } else {
            try {
                projectDir = new File(".").getCanonicalPath();
            } catch (IOException ex) {
                System.out.println("unable to locate projectDir - " + ex);
                System.exit(-1);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        fillProjectDir();
        if (args.length > 0) {
            consoleRead(args);
        }
        try {
            startProgram();

        } catch (LoggerException ex) {
            System.err.println("logger error - " + ex);
        } catch (Exception ex) {
            try {
                if (globalLogger == null) {
                    System.err.println("logger is null, unable log");
                } else {
                    globalLogger.log("unexpected error - see folder project/logs/global/exceptions for more information", ex);
                }
            } catch (Exception ex1) {
                System.err.println("unexpected error - " + ex1.getMessage());
            }
        }
        endProgram();
    }

    public static void startProgram() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Starting PDFCreator at " + sdf.format(new Date()));
        lock = new File(projectDir + s + ".lock");
        try {
            loggerConfig = new Config(projectDir + s + CONFIG_FOLDER + s + "logger.yml");
        } catch (ConfigException ex) {
            System.err.println(ex.getMessage());
        }
        globalLogger = new Logger(projectDir, "global", loggerConfig);
        try {
            globalConfig = new Config(projectDir + s + CONFIG_FOLDER + s + "global.yml");
            magicConfig = new Config(projectDir + s + CONFIG_FOLDER + s + ".magic.yml");
        } catch (ConfigException ex) {
            globalLogger.log("unable to read global config file", ex);
        }

        if (lock.exists()) {
            globalLogger.simpleLog("terminating... another instance is running (.lock file exists)");
            try {
                ObjectInputStream inObject = new ObjectInputStream(new FileInputStream(projectDir + s + "lastrun.time"));
                Date date = (Date) inObject.readObject();

                inObject.close();
                Date now = new Date();
                int minDelay = (int) ((now.getTime() - date.getTime()) / 60000);
                globalLogger.simpleLog("last run at " + globalLogger.TIME_FORMAT.format(date) + " (" + minDelay + " min)");
                if (minDelay > globalConfig.getInt("maxDelay")) {
                    globalLogger.log("program isnt runnig due to unexpected crash, last run at: " + globalLogger.TIME_FORMAT.format(date) + " deleting .lock file");
                    lock.delete();
                    System.exit(0);
                } else {
                    System.exit(0);
                }

            } catch (IOException | ClassNotFoundException ex) {
                globalLogger.log("unable to read lastrun.time file ", ex);
            }

            System.exit(-1);
        }
        try {
            lock.createNewFile();
        } catch (IOException ex) {
            globalLogger.log("unable to create lock file - ", ex);
            System.exit(-1);
        }
        try {
            ArrayList apps = globalConfig.getArrayList("apps");
            for (int i = 0; i < apps.size(); i++) {
                startApp((String) apps.get(i));
            }
        } catch (JSONException ex) {
            globalLogger.log("unable to read global config file - " + ex.getMessage(), ex);
            exit();
        }

    }
    //init end

    //one app
    public static void startApp(String id) {
        System.out.println("starting " + id);
        try {

            config = new Config(projectDir + s + CONFIG_FOLDER + s + id + ".yml");

        } catch (ConfigException ex) {
            globalLogger.log("config for app \"" + id + "\" not found or corrupted - " + ex.getMessage(), ex);
            return;
        }
        logger = new Logger(projectDir, id, loggerConfig);
        try {
            Config paths = config.getConfig("paths");
            templatesPath = paths.getString("templates");
            imagesPath = paths.getString("images");
            outputsPath = paths.getString("outputs");
            fontsPath = paths.getString("fonts");
            basePath = paths.getString("basePath");
            enableUploadToFTP = config.getBoolean("enableUploadToFTP");
            enableMoveGeneratedFiles = config.getBoolean("enableMoveGeneratedFiles");
            deleteOldFilesAfter = config.getInt("deleteOldFilesAfter");

            doApp();

        } catch (JSONException ex) {
            logger.log("JSON parse error check config " + id + ".json - " + ex.getMessage(), ex);
        }

    }

    public static void fillDataSet(String url) throws IOException {
        if (checkFonts | dryRunNoData) {
            try {
                data = new JSONArray(new JSONTokener(new FileInputStream(projectDir + s + globalConfig.getString("dryRunJSON"))));
            } catch (FileNotFoundException ex) {
                logger.log("cant read dry run data", ex);
                exit();

            }
        } else {
            URL dataStream = new URL(url);
            URLConnection connection = dataStream.openConnection();
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                int code = httpConnection.getResponseCode();
                //ignore 503 response
                if (code == HttpURLConnection.HTTP_UNAVAILABLE) {
                    data = null;
                } else {
                    data = new JSONArray(new JSONTokener(dataStream.openStream()));
                }

            } else {
                throw new IOException("error - not a http request!");
            }

        }

    }

    public static void doApp() {
        String url = "";
        try {
            url = config.getConfig("source").getString("url");
            fillDataSet(url);
            if (jsonDump) {
                System.out.println(data);
            }
            if (data == null) {
                System.out.println("api is not available");
                return;
            }
            if (data.length() == 0) {
                System.out.println("no orders to create");
                return;
            }
            if (enableUploadToFTP) {
                if (!connectToFTP()) {
                    return;
                }
            }

        } catch (IOException | JSONException ex) {

            logger.log("unable to read data from " + url, ex, "unable to generate pdfs");
            return;
        }
        try {
            registerFonts();

            for (int i = 0; i < data.length(); i++) {
                if (!doOnePdf(getData(i))) {// else zaznamenat do response
                    deleteDocument();
                } else {
                    boolean upError = false;
                    boolean cpError = false;
                    if (enableUploadToFTP) {
                        if (!upload()) {
                            upError = true;
                        }
                    }
                    if (enableMoveGeneratedFiles) {
                        String name = pdfName + pdf;
                        try {
                            String destFolder = config.getConfig("paths").getString("moveOutputsToFolder");
                            String src = outputsPath + s + name;
                            String dest = destFolder + s + name;
                            System.out.println("moving " + name + " to " + destFolder);
                            if (!copyFile(src, dest)) {
                                cpError = true;
                            }

                        } catch (JSONException ex) {
                            logger.log("can't move generated file -" + ex.getMessage(), ex, "unable to read pdf");
                        }
                    }
                    if (!(enableUploadToFTP || enableMoveGeneratedFiles)) {
                        logger.log("warning: enableUploadToFTP = false and enableMoveGeneratedFiles = false, generated pdf will be in " + outputsPath);
                    }

                    if (!(upError || cpError)) {
                        response.put(pdfId);
                        if (!dryRun) {
                            sendPut(response, url);
                            logger.logCreatedOrder(pdfName, "" + pdfId);
                        } else {
                            printDebug("dryrun, response " + response + " not send ");
                        }
                        response.clear();
                    } else {
                    }
                }
            }

        } catch (DocumentException | ExceptionConverter ex) {
            String message = "document error - " + ex.getMessage();
            logger.log(message, ex, ER_NODOC + " id = " + pdfId);
            putErrorResponse(pdfId, message);
            deleteDocument();
        } catch (JSONException ex) {
            String message = "JSON data error, invalid data - " + ex.getMessage();
            logger.log(message, ex, ER_NODOC + " id = " + pdfId);
            putErrorResponse(pdfId, message);
            deleteDocument();
        }

        //po dokonceni vsech pdf
        if (enableUploadToFTP) {
            disconnect();
        }

        sendErrorResponse();

        deleteOldFiles(outputsPath);

    }

    public static boolean copyFile(String from, String to) {
        try {
            Files.copy(new File(from).toPath(), new File(to).toPath(), REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.log("unable to copy generated from " + from + " to " + to, ex);
            return false;
        }
        return true;
    }

    public static void registerFonts() {
        fontProvider = new XMLWorkerFontProvider(fontsPath);
        fontProvider.defaultEncoding = BaseFont.IDENTITY_H;
        notUsedFonts.add("times");
        notUsedFonts.add("times-roman");
        notUsedFonts.add("zapfdingbats");
        notUsedFonts.add("symbol");
        notUsedFonts.add("helvetica");
        notUsedFonts.add("courier");
    }

    public static void deleteDocument() {
        try {
            if (fileStream != null) {
                File del = new File(outputsPath + s + pdfName + pdf);
                fileStream.close();
                del.delete();
                // logger.log("pdf " + pdfName + " was not created");
            }

        } catch (IOException ex) {
            logger.log("cant close documnet output stream - " + ex.getMessage(), ex);
        }
    }

    //one app end
    //one pdf
    /**
     *
     * @param data - JSON data
     * @return true if whole documnet is created, false otherwise
     * @throws DocumentException - documnet error
     * @throws JSONException - JSON error
     */
    static boolean doOnePdf(JSONObject data) throws DocumentException, JSONException {
        pdfName = data.getString("serialNumber");
        pdfId = data.getInt("id");
        if (checkFonts) {
            pdfName = "font_test";
        }
        compression = data.getFloat("quality");
        defaultFont = data.getString("defaultTextFont");
        document = new Document();
        try {

            fileStream = new FileOutputStream(outputsPath + s + pdfName + pdf);
            writer = PdfWriter.getInstance(document, fileStream);
            //writer.setInitialLeading(0);

        } catch (FileNotFoundException ex) {
            String message = "cant write to document - " + ex.toString();
            logger.log(message, ex, ER_NODOC + " id = " + pdfId);
            putErrorResponse(pdfId, message);
            return false;
        }
        document.open();
        document.addAuthor(config.getString("name")+", wavevision s.r.o.");
        JSONArray pages = data.getJSONArray("pages");

        pages = sortPages(pages);
        for (int i = 0; i < pages.length(); i++) {
            JSONObject page = pages.getJSONObject(i);
            width = mmToUnit(page.getFloat("width"));
            height = mmToUnit(page.getFloat("height"));
            bleed = mmToUnit(page.getFloat("bleed"));
            productId = data.getInt("productId");
            widthBl = width + 2 * bleed;
            heightBl = height + 2 * bleed;
            document.setPageSize(new Rectangle(widthBl, heightBl));
            document.newPage();

            if (!addPage(page)) {
                return false;
            }

        }
        document.close();
        String pdfPathName = outputsPath + s + pdfName + pdf;
        System.out.println(pdfPathName + " was created");
        return true;
    }
    //one pdf end

    //one page
    public static boolean addPage(JSONObject page) throws DocumentException {
        cb = writer.getDirectContentUnder();
        if (!page.isNull("fieldInstances")) {

            if (!drawFieldInstances(page.getJSONArray("fieldInstances"), width, height)) {
                return false;
            }
        }
        if (!page.isNull("template")) {
            try {
                templateImage = Image.getInstance(basePath + s + templatesPath + s + productId + s + page.getString("template"));
                templateImage.setAbsolutePosition(0, 0);
                templateImage.scaleAbsolute(widthBl, heightBl);
                cb.addImage(templateImage);
            } catch (BadElementException | IOException ex) {
                String message = "unable to load template - " + ex.getMessage();
                logger.log(message, ex, ER_NODOC + " id = " + pdfId);
                putErrorResponse(pdfId, message);
                return false;

            }
        }
        try {
            cb = writer.getDirectContent();
            //  registerFonts();
            addTextIntances(page, width, height);
        } catch (IOException | DocumentException ex) {
            String message = "text error - " + ex;
            logger.log(message, ex, ER_NODOC + " id = " + pdfId);
            putErrorResponse(pdfId, message);
            return false;
        }

        return true;
    }

    /**
     *
     * @param data JSON data
     * @param width document width to calculate pdf unit values from JSON
     * relative values
     * @param height document width to calculate pdf unit values from JSON
     * relative values
     * @throws java.io.IOException
     * @throws com.itextpdf.text.DocumentException
     */
    public static void addTextIntances(JSONObject data, float width, float height) throws IOException, DocumentException {
        if (!data.isNull("textInstances")) {
            JSONArray texts = data.getJSONArray("textInstances");
            JSONObject text;
            String html;
            float posX, posY, textWidth, textHeight;
            for (int i = 0; i < texts.length(); i++) {
                text = texts.getJSONObject(i);
                if (!text.getBoolean("hasBeenChanged")) {
                    continue;
                }
                float size = text.getFloatP("fontSize") * height;
                if (!fontProvider.isRegistered(defaultFont)) {
                    String backupFont = config.getString("defaultFont");
                    logger.log("font \"" + defaultFont + "\" is not registered, using default font instead (" + backupFont + ")");
                    defaultFont = backupFont;
                }
                html = text.getString("textToExport");

                html = html.replaceAll("<br>", "<br/>");
                String reg = "font-size[ ]*:[ ]*(\\d+)[ ]*%;";
                Pattern p = Pattern.compile(reg);
                Matcher m = p.matcher(html);
                StringBuffer buf = new StringBuffer();
                while (m.find()) {
                    String res = m.group(1);
                    float f = parseFloat(res);
                    String rep = Integer.toString((int) (f * size));
                    float lineHeight = f*size*1.0f;
                    m.appendReplacement(buf, "line-height: " + lineHeight + "pt; font-size: " + rep + "pt;");
                }
                m.appendTail(buf);
                html = buf.toString();
         

                html = "<body style=\"color: rgb(0, 0, 0); font-size: " + (int) size + "pt; text-align: " 
                        + text.getString("textAlign") + "; font-family: " + defaultFont +
                        ";\"><p style=\"margin:0; padding:0;\">" + html + "</p></body>";
                
                html = html.replaceAll("<div", "<p style=\"margin:0; padding:0;\"");
                html = html.replaceAll("</div>", "</p>");
                html = html.replaceAll("style=\"line-height[ ]*:[ ]*(\\d+)[ ]*px;\"", ""); //todo relativne
                html = html.replaceAll("background-color: transparent;", "");
                //  html = html.replaceAll("<[img].[^>]*>", "");
                int rotation = text.getInt("rotation");
                float heightMagic = magicConfig.getFloat("fieldHeightFix");
                float paddingFix = magicConfig.getFloat("paddingFix");
                float dataX = text.getFloatP("x");
                float dataY = text.getFloatP("y")-paddingFix;
                posX = dataX * width;
                textWidth = text.getFloatP("width") * width;
                textHeight = text.getFloatP("height") * height * (1 + heightMagic);
                
                posY = ((1 - dataY) * height) - textHeight;
                addText(html, posX + bleed, posY + bleed, textWidth, textHeight, rotation, size);

            }
        }
    }

    /**
     *
     * @param html
     * @param posX
     * @param posY
     * @param width
     * @param height
     * @param rotation
     * @throws java.io.IOException
     * @throws com.itextpdf.text.DocumentException
     */
    public static void addText(String html, float posX, float posY, float width, float height, int rotation, float baseMagic) throws IOException, DocumentException {
        PdfTemplate template = cb.createTemplate(0, 0);
        float small = width * magicConfig.getFloat("paddingFix");
        float rotMagic = baseMagic * magicConfig.getFloat("rotFix1");
        float rotMagic2 = baseMagic * magicConfig.getFloat("rotFix2");
        float rotMagic3 = baseMagic * magicConfig.getFloat("rotFix3");
        width = width - small;
        template.setBoundingBox(new Rectangle(0, 0, width, height));
        ColumnText ct = new ColumnText(template);
        ct.setSimpleColumn(0, 0, width, height);
        posX += small / 2;

        ByteArrayInputStream in = new ByteArrayInputStream(html.getBytes());
        HtmlPipelineContext htmlContext = new HtmlPipelineContext(new CssAppliersImpl(fontProvider));
        htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());
        CSSResolver cssResolver = XMLWorkerHelper.getInstance().getDefaultCssResolver(true);
        Pipeline<?> pipeline = new CssResolverPipeline(cssResolver, new HtmlPipeline(htmlContext,
                new ElementHandlerPipeline(new ElementHandler() {
                    @Override
                    public void add(final Writable w) {
                        if (w instanceof WritableElement) {
                            List<Element> elements = ((WritableElement) w).elements();
                            el.addAll(elements);
                        }
                    }
                }, null)));
        XMLWorker worker = new XMLWorker(pipeline, true);

        XMLParser p = new XMLParser(worker);
        try {
            p.parse(in);
        } catch (RuntimeWorkerException ex) {
            logger.log("malformed html - " + ex.getMessage(), ex);
        }
        for (Element element : el) {
            ct.addElement(element);
        }
        ct.go();
        Image img = Image.getInstance(template);

        img.setRotationDegrees(-rotation);
        switch (rotation) {
            case 90: {
                img.setAbsolutePosition(posX + rotMagic2 + (width / 2) - (height / 2), posY - (width / 2) + (height / 2) - small);
                break;
            }
            case 270: {
                img.setAbsolutePosition(posX - rotMagic3 + (width / 2) - (height / 2), posY - (width / 2) + (height / 2) - small);
                break;
            }
            case 180: {

                img.setAbsolutePosition(posX, posY - rotMagic);
                break;
            }
            case 0: {
                img.setAbsolutePosition(posX, posY);
                break;
            }
            default: {
                logger.log("undefined rotation of text, no text added (supported rotations 0,90,180,270)");
                img.setAbsolutePosition(posX, posY);
                break;
            }
        }
        document.add(img);

        el.clear();
    }

    public static boolean drawFieldInstances(JSONArray fields, float width, float height) {
        JSONObject field;
        PdfGraphics2D graphics = new PdfGraphics2D(cb, widthBl, heightBl, null, true, true, compression);
        java.awt.Image image;
        JSONObject imageJSON;
        BufferedImage testImage;
        JSONObject relativePositionJSON;
        for (int i = 0; i < fields.length(); i++) {
            field = fields.getJSONObject(i);
            if (!field.isNull("fieldInstanceImage")) {

                imageJSON = field.getJSONObject("fieldInstanceImage");
                String imageName = imageJSON.getString("image");
                String pathName = basePath + s + imagesPath + s + imageName;
                try {
                    image = Toolkit.getDefaultToolkit().getImage(pathName);
                    testImage = ImageIO.read(new File(pathName));

                } catch (IOException ex) {
                    String message = "Image " + pathName + " not found - " + ex.getMessage();
                    logger.log(message, ex, ER_NODOC + " id = " + pdfId);
                    putErrorResponse(pdfId, message);
                    graphics.dispose();
                    return false;
                }
                if (imageJSON.getBoolean("isFlipped")) {
                    try {
                        imageName = imageName.replaceAll("\\.", "_flipped.");
                        String backupPathName = basePath + s + imagesPath + s + imageName;
                        MediaTracker m = new MediaTracker(new JPanel());
                        m.addImage(image, 0);
                        m.waitForAll();
                        BufferedImage backupImage = new BufferedImage(testImage.getWidth(), testImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                        Graphics g = backupImage.getGraphics();
                        g.drawImage(image, 0, 0, null);
                        g.dispose();
                        backupImage = horizontalflip(backupImage);
                        ImageIO.write(backupImage, "JPEG", new File(backupPathName));
                        image = Toolkit.getDefaultToolkit().getImage(backupPathName);
                    } catch (IOException | InterruptedException ex) {
                        String message = "unable to flip image " + pathName + " - " + ex.getMessage();
                        logger.log(message, ex, ER_NODOC + " id = " + pdfId);
                        putErrorResponse(pdfId, message);
                        graphics.dispose();
                        return false;
                    }
                }
                float clipX, clipY, clipWidth, clipHeight, posX, posY;
                int relWidth, relHeight;
                final String type = field.getString("type");
                clipX = (width * field.getFloatP("x")) + bleed;
                clipY = (height * field.getFloatP("y")) + bleed;
                clipWidth = width * field.getFloatP("width");
                clipHeight = height * field.getFloatP("height");
                relativePositionJSON = imageJSON.getJSONObject("relativePosition");
                posX = (clipWidth * (relativePositionJSON.getFloat("x")) + clipX);
                posY = (clipHeight * (relativePositionJSON.getFloat("y")) + clipY);
                double relHeightFloat = clipHeight * imageJSON.getFloat("relativeHeight");
                relHeight = (int) Math.ceil(relHeightFloat);

                double sideRatio = (float) (testImage.getWidth(null)) / (float) testImage.getHeight(null);
                relWidth = (int) Math.ceil(sideRatio * relHeightFloat);

                switch (type) {
                    case ShapeType.RECTANGLE: {
                        graphics.setClip(new Rectangle2D.Float(clipX, clipY, clipWidth, clipHeight));
                        break;
                    }
                    case ShapeType.ELLIPSE: {
                        graphics.setClip(new Ellipse2D.Float(clipX, clipY, clipWidth, clipHeight));
                        break;
                    }
                    case ShapeType.POLYGON: {
                        String csv = "";
                        try {
                            csv = field.getString("points");
                            String p[] = csv.split(" ");
                            int x[] = new int[p.length];
                            int y[] = new int[p.length];
                            String pp[];

                            for (int ii = 0; ii < p.length; ii++) {
                                pp = p[ii].split(",");
                                x[ii] = (int) Math.ceil((Float.parseFloat(pp[0]) * clipWidth) + clipX);
                                y[ii] = (int) Math.ceil((Float.parseFloat(pp[1]) * clipHeight) + clipY);
                            }
                            Shape poly = new Polygon(x, y, x.length);
                            if (x.length < 3 | y.length < 3) {
                                String message = "unable to create polygon from less than 3 points  - " + csv;
                                logger.log(message, null, ER_NODOC + " id = " + pdfId);
                                putErrorResponse(pdfId, message);
                                graphics.dispose();
                                return false;
                            }
                            graphics.setClip(poly);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            String message = "invalid polygon CSV - " + csv;
                            logger.log(message, ex, ER_NODOC + " id = " + pdfId);
                            putErrorResponse(pdfId, message);
                            graphics.dispose();
                            return false;
                        }

                        break;
                    }
                    default: {
                        logger.log("unknown clip shape :" + type);
                        graphics.dispose();
                        return false;
                    }
                }

                //rotace podle stredu obrazku
                graphics.rotate(Math.toRadians(imageJSON.getInt("rotation")), posX + (relWidth / 2), posY + (relHeight / 2));
                graphics.drawImage(image, (int) Math.floor(posX), (int) Math.floor(posY), relWidth, relHeight, null);
                graphics.rotate(-Math.toRadians(imageJSON.getInt("rotation")), posX + (relWidth / 2), posY + (relHeight / 2));

                // rotace podle stredu clipu
              /*   graphics.rotate(Math.toRadians(imageJSON.getInt("rotation")), clipX + (clipWidth / 2), clipY + (clipHeight / 2));
                 graphics.drawImage(image, (int) Math.floor(posX), (int) Math.floor(posY), relWidth, relHeight, null);
                 graphics.rotate(Math.toRadians(-imageJSON.getInt("rotation")), clipX + (clipWidth / 2), clipY + (clipHeight / 2));*/
            }
        }
        graphics.dispose();
        return true;
    }

    public static JSONObject getData(int index) {
        if (checkFonts) {
            try {
                String html = "";
                String text = "příliš žluťoučký kůň úpěl ďábelské ódy";

                File f = new File(fontsPath);

                Set<String> fonts = fontProvider.getRegisteredFamilies();

                fonts.removeAll(notUsedFonts);

                for (String fontName : fontProvider.getRegisteredFamilies()) {
                    html += "<p style=\" font-size: 11pt;font-family:" + fontName + ";\">font-family: " + fontName + "<br/>"
                            + "" + text + ", " + text.toUpperCase() + "<br/><b>" + text + ", " + text.toUpperCase() + "</b>"
                            + "<br/><i>" + text + ", " + text.toUpperCase() + "</i></p>";
                }
                System.out.println("font files in :" + fontsPath);
                for (File ff : f.listFiles()) {
                    printDebug(ff.toString());
                }

                if (fonts.isEmpty()) {
                    System.err.println("no fonts registered");

                } else {
                    System.out.println("registered fonts:");
                    for (Object name : fonts) {
                        System.out.println(name);
                    }
                }
                JSONArray json = new JSONArray(new JSONTokener(new FileInputStream(projectDir + s + globalConfig.getString("fontCheckJSON"))));
                JSONObject obj = json.getJSONObject(0);
                JSONObject a = obj.getJSONArray("pages").getJSONObject(0).getJSONArray("textInstances").getJSONObject(0);
                html = "<p style=\"font-size: 14pt;\">" + config.getString("name") + ": test fontů</p>" + html;
                a.put("textToExport", html);
                return obj;
            } catch (FileNotFoundException ex) {
                logger.log("cant read dry run data", ex);
                exit();
                return null;
            }

        } else {

            return data.getJSONObject(index);
        }
    }

    //one page end
    //misc
    public static void endProgram() {
        lock.delete();
        Date lastRun = new Date();
        try {
            FileOutputStream out = new FileOutputStream(projectDir + s + "lastrun.time");
            ObjectOutputStream object = new ObjectOutputStream(out);
            object.writeObject(lastRun);
            object.close();
            out.close();
        } catch (IOException ex) {
            System.err.println("unable to note last run date - " + ex);
        }
    }

    public static void exit() {
        System.out.println("see error logs");
        lock.delete();
        System.exit(-1);
    }

    public static void endApp(String cause) {
        logger.log(cause);
        lock.delete();
    }

    public static float mmToUnit(float milimeters) {
        return milimeters * 2.834645669f;
    }
    //misc end

    //ftp
    public static boolean connectToFTP() {

        ftp = new FTPClient();
        ftpInfo = config.getConfig("ftp");
        host = ftpInfo.getString("host");
        printDebug("connecting to " + host);
        user = ftpInfo.getString("user");
        printDebug("user: " + user);
        password = ftpInfo.getString("password");
        location = ftpInfo.getString("location");
        printDebug("location: " + location);
        try {
            int reply;
            ftp.connect(host);
            ftp.login(user, password);
            reply = ftp.getReplyCode();

            if (FTPReply.isPositiveCompletion(reply)) {
                ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
                ftp.setFileTransferMode(FTPClient.BINARY_FILE_TYPE);

            } else {
                logger.log("login to " + host + " failed  reply: " + reply);
                return false;
            }
        } catch (java.net.UnknownHostException ex) {
            logger.log("unknown host " + host, ex);
            return false;

        } catch (SocketException ex) {
            logger.log("fpt err.  -" + ex.getMessage(), ex);
            return false;
        } catch (IOException ex) {
            logger.log("fpt err. - " + ex.getMessage(), ex);
            return false;
        }
        printDebug("connection ok");
        return true;
    }

    public static boolean upload() {

        printDebug("uploading...");

        String fileDir = outputsPath + s + pdfName + pdf;
        String ftpDir = location + s + pdfName + pdf;

        try {
            FileInputStream in = null;
            final File f = new File(fileDir);
            in = new FileInputStream(f);
            BufferedInputStream in2 = new BufferedInputStream(in);
            ftp.storeFile(ftpDir, in2);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                String stringReply = ftp.getReplyString();
                if (FTPReply.STORAGE_ALLOCATION_EXCEEDED == reply) {
                    logger.log("ftp full, reply = " + reply + ", message = " + stringReply);
                } else {
                    logger.log("ftp error, reply = " + reply + ", message = " + stringReply);
                }
                return false;
            }

            in.close();
        } catch (IOException ex) {
            logger.log("unable to upload file to " + host, ex);
            return false;
        }
        printDebug("done");
        return true;

    }

    public static boolean disconnect() {
        try {
            ftp.disconnect();
            printDebug("ftp connection closed");
            return true;

        } catch (IOException ex) {
            logger.log("error during disconnecting from " + host, ex);
            return false;
        }

    }

    public static float parseFloat(String percent) {
        percent = percent.replaceAll("%", "");
        return Float.parseFloat(percent) / 100;
    }

    public static BufferedImage horizontalflip(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dimg = new BufferedImage(w, h, img.getType());
        Graphics2D g = dimg.createGraphics();
        g.drawImage(img, 0, 0, w, h, w, 0, 0, h, null);
        g.dispose();
        return dimg;
    }

    private static void sendPut(JSONArray params, String url) {
        int responseCode;
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("PUT");
            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(params.toString());
            wr.flush();
            wr.close();

            responseCode = con.getResponseCode();
            printDebug("response: " + responseCode);
            //debug
           /* BufferedReader in = new BufferedReader(
             new InputStreamReader(con.getInputStream()));
             String inputLine;
             StringBuffer response = new StringBuffer();

             while ((inputLine = in.readLine()) != null) {
             response.append(inputLine);
             }
             in.close();

             //print result
             System.out.println(response.toString());*/

        } catch (IOException ex) {
            logger.log("unable to send response to " + url, ex, "no new pdf will be created");
            return;
        }
        if (responseCode != 200) {
            logger.log("unable to send response to " + url + ", response = " + responseCode, null, "no new pdf will be created");
        }

    }

    public static void deleteOldFiles(String folderPath) {
        int maxDays = deleteOldFilesAfter;
        if (maxDays != -1) {
            System.out.println("deleting old files from " + folderPath);
            long treshold = maxDays * 1000 * 60 * 60 * 24;
            Date now = new Date();
            for (File file : new File(folderPath).listFiles()) {
                if (now.getTime() - file.lastModified() > treshold) {
                    file.delete();
                }
            }
        }
    }

    public static void sendErrorResponse() {
        printDebug("responseError:");
        printDebug(responseError.toString());
        if (responseError.length() != 0) {
            Config source = config.getConfig("source");
            sendPut(responseError, source.getString("url") + "/" + source.getString("error"));

        }
    }

    public static void putErrorResponse(int id, String message) {
        JSONObject er = new JSONObject();
        String dir = projectDir + "/orderErrors";
        File erDir = new File(dir);
        if (!erDir.exists()) {
            erDir.mkdir();
        }
        String name = dir + "/" + "order_" + id;
        File file = new File(name);
        OrderError error = new OrderError(id, message);
        OrderError compare;
        if (file.exists()) {
            FileInputStream inStream;
            ObjectInputStream inObjectStream;
            try {
                inStream = new FileInputStream(file);
                inObjectStream = new ObjectInputStream(inStream);
                compare = (OrderError) inObjectStream.readObject();
                if (!compare.isSend()) {
                    er.put("id", id);
                    er.put("message", message);
                    responseError.put(er);
                    compare.setSend(true);
                    saveOrderError(compare, file);
                }

            } catch (IOException | ClassNotFoundException ex) {
                logger.logNoMail("cant read errors", ex);
            }

        } else {
            saveOrderError(error, file);
        }
    }

    private static void saveOrderError(OrderError error, File file) {
        ObjectOutputStream objectStream;
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(file);
            objectStream = new ObjectOutputStream(outStream);
            objectStream.writeObject(error);
            outStream.close();
            objectStream.close();
        } catch (IOException ex1) {
            logger.logNoMail("unable to serialize order error", ex1);
        }

    }

    static void printDebug(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }

    static JSONArray sortPages(JSONArray pages) {
        JSONArray newPages = new JSONArray();
        LinkedHashMap<Integer, JSONObject> map = new LinkedHashMap<>();
        for (int i = 0; i < pages.length(); i++) {
            map.put(pages.getJSONObject(i).getInt("index"), pages.getJSONObject(i));
        }
        SortedSet<Integer> keys = new TreeSet<>(map.keySet());
        for (Integer key : keys) {
            newPages.put(map.get(key));
        }
        return newPages;
    }
    //ftp end
}

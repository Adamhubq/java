package com.company;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

public class Main {
    public String result;
    public static int arifmSum = 0;
    public static String user;
    public static String pass;
    public static String connect;
    public static int num_con;
    public static int col_vo_strokInt;

    static class timer extends TimerTask{

        @Override
        public void run() {
            System.exit(0);
        }
    }


    public static void main(String[] args) throws  ClassNotFoundException {
        setConnect("jdbc:mysql://localhost:3306/test?useSSL=false");
        setUser("root");
        setPass("1234");
        Class.forName("com.mysql.jdbc.Driver");

        try (Connection connection = DriverManager.getConnection(connect, user, pass);
             Statement statement = connection.createStatement()
        ) {

            ArrayList<Integer> parseInXml = new ArrayList<Integer>();
            Scanner col_vo_strok;

            System.out.println("Введите число N");
            while (true) {
                col_vo_strok = new Scanner(System.in);
                try {
                    col_vo_strokInt = col_vo_strok.nextInt();
                    if (col_vo_strokInt == 0 ) {
                        System.out.println("0 строк не может быть создано. N должно быть больше 0");
                    }
                    else {
                        setNum_con(col_vo_strokInt);
                        break;
                    }
                } catch (Throwable t) {
                    System.out.println("Неверный тип данных. Введите число");
                }
            }
            Timer timer = new Timer();
            timer.schedule(new timer(), 5*60*1000);
            ResultSet select = statement.executeQuery("SELECT test.number from test;");
            if (select.next()) { // проверяем пустоту таблицы
                statement.executeUpdate("TRUNCATE TABLE test;");
                System.out.println("таблица очищена, press enter");
                System.in.read();
            }

            String request = SQL_requestInsert(getNum_con()); // генерируем запрос
            statement.executeUpdate(request);             // и записываем
            System.out.println("днные записаны в табоицк");

            select = statement.executeQuery("SELECT test.number from test;");
            while (select.next()) {
                parseInXml.add(select.getInt(1));
            }

            createXML(parseInXml);
            System.out.println("Файл 1.xml создан, press enter");
            System.in.read();
            Main c = new Main();
            final String xml = "1.xml";
            final String xsl = "xslt.xsl";

            try {
                c.result = c.xml1ToXml2String(xml, xsl);
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }

            File  source = new File ("1.xml");
            source.renameTo(new File("2.xml"));
            createXML(c.result, "2.xml");
            num_con = 0;
            DefaultHandler handler = new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    super.startElement(uri, localName, qName, attributes);
                    String field = attributes.getValue("field");
                    if (field != null && !field.isEmpty()) {
                        arifmSum += Integer.parseInt(field);

                    }
                }
            };

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new File("2.xml"), handler);
            System.out.println("Сумма всех элементов = " + arifmSum);
            System.exit(0);

        } catch (SQLException sql) {
            sql.printStackTrace(System.out);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        } catch (Throwable t){
            t.printStackTrace(System.out);
        }
    }
    public static String createXML(ArrayList parseInXml) throws TransformerException, IOException, ParserConfigurationException {

        deleteFileIfExist("1.xml", "2.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element entries = document.createElement("entries");
        document.appendChild(entries);

        for (int i = 0; i < parseInXml.size(); i++) {
            Element entry = document.createElement("entry");
            Element field = document.createElement("field");
            Text text  = document.createTextNode(parseInXml.get(i).toString());
            entries.appendChild(entry);
            entry.appendChild(field);
            field.appendChild(text);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(document), new StreamResult(new FileOutputStream("1.xml")));
        return String.valueOf(transformer);
    }
    public static void deleteFileIfExist(String file_name1, String file_name2) throws IOException {

        File file_1xml = new File(file_name1);
        File file_2xml = new File(file_name2);

        if (file_1xml.exists() || file_2xml.exists()) {
            file_1xml.delete();
            file_2xml.delete();
            System.out.println("Удален файл xml, press enter");
            System.in.read();
        }
    }

    public static String createXML(String data, String fileName) throws IOException {
        FileWriter wr = new FileWriter(fileName, false);
        wr.write(data);
        wr.close();
        return String.valueOf(wr);
    }

    public static String SQL_requestInsert(int N) {         // Создаем скрипт SQL для
        String Insert = "Insert into test (FIELD) value";   // создания записей в таблицк

        for (long i = 1; i < N; i++) {                      // инкремент long так как на вход подается любое целочисленное
            Insert += "(" + i + "),";
        }

        Insert += "(" + N + ");";
        return Insert;
    }

    public String xml1ToXml2String(String xmlFile, String xslFile) throws Exception {

        InputStream xmlFile_doc = new FileInputStream(xmlFile);                                 // Открыть файлы
        InputStream xslFile_doc = new FileInputStream(xslFile);                                 // в виде потоков
        StreamSource xmlSource = new StreamSource(xmlFile_doc);                                 // Создать источник
        StreamSource stylesource = new StreamSource(xslFile_doc);                               // для транформации из потоков
        ByteArrayOutputStream bos = new ByteArrayOutputStream();                                // Создем байтовый поток для результата
        StreamResult xmlOutput = new StreamResult(bos);                                         // Создаем приемноик для результатат из потока
        Transformer transformer = TransformerFactory.newInstance().newTransformer(stylesource); // Создаем трансформатор
        transformer.transform(xmlSource, xmlOutput);                                            // и выполняем трансформацию
        return bos.toString();
    }

    public static void setNum_con(int num_con) {
        Main.num_con = num_con;
    }

    public static int getNum_con() {
        return num_con;
    }

    public static void setPass(String pass) {
        if (pass == null) {
            return;
        }
        Main.pass = pass;
    }

    public static void setConnect(String connect) {
        if (connect == null) {
            return;
        }
        Main.connect = connect;
    }

    public static void setUser(String user) {
        if (user == null) {
            return;
        }
        Main.user = user;
    }
}
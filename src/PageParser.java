import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

class PageParser {
    private static PageParser pageParser = null;
    private static final String HOST = "http://www.bttiantang.com";
    private static final String REQUEST_URL = "http://www.bttiantang.com/download4.php";
    private ConfigLoader configLoader = ConfigLoader.getInstance();
    private static ArrayList<String> targetCategoriesSubURLs = new ArrayList<>();
    private static ArrayList<String> validFilmTitles = new ArrayList<>();
    private static ArrayList<String> validFilmSubURLs = new ArrayList<>();

    static synchronized PageParser getInstance() {
        if (pageParser == null)
            pageParser = new PageParser();
        return pageParser;
    }

    private PageParser() {
        System.out.print("[o] Connecting...");
        try {
            URL url = new URL(HOST);
            Document document = Jsoup.parse(url, 5000);
            System.out.println("OK");
            System.out.println("[o] Collecting target categories:");
            Elements bTitles = document.select("div[class=\"Btitle\"]");
            for (Element each : bTitles) {
                Element thisCategory = each.select("a").first();
                ArrayList<String> categories = configLoader.getCategories();
                if (!categories.contains(thisCategory.text()))
                    continue;
                targetCategoriesSubURLs.add(thisCategory.attr("href"));
                System.out.println("[o] \tCategory ["+thisCategory.text()+"] spotted");
            }
        } catch (MalformedURLException e) {
            System.out.println("\n[x] Internal error: MalformedURL");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("\n[x] An error occurred when trying to read the page");
            System.exit(0);
        }
        parseCategoryPage();
    }

    void parseCategoryPage() {
        System.out.println("[o] You are banning films from "+configLoader.getRegions_banned().toString());
        System.out.println("[o] You want to dig into each category with the depth of: "+configLoader.getDepth());
        if (configLoader.getDepth() < 1) {
            System.out.println("[x] Depth can not be smaller than 1");
            System.exit(0);
        }
        else if (configLoader.getDepth() > 5) {
            System.out.println("[i] Depth may be too large");
        }
        System.out.print("[o] Collecting films into each category...");
        int counterDuplicated = 0;
        int counterBanned = 0;
        boolean isBanned;
        boolean isDuplicated;
        for (String each : targetCategoriesSubURLs) {
            for (int i = 1; i < configLoader.getDepth() + 1; i++) {
                try {
                    URL url = new URL(HOST + each + i);
                    Document document = Jsoup.parse(url, 5000);
                    Elements filmTitles = document.select("div[class=\"title\"]");
                    for (Element eachFilmTitle : filmTitles) {
                        if (!"".equals(eachFilmTitle.select("font").text())) {
                            isBanned = false;
                            isDuplicated = false;
                            for (String eachBannedLocation : configLoader.getRegions_banned()) {
                                if (eachFilmTitle.select("p[class=\"des\"]").text().contains(eachBannedLocation)) {
                                    counterBanned++;
                                    isBanned = true;
                                }
                            }
                            for (String eachValidFileTitle : validFilmTitles) {
                                if (eachFilmTitle.select("font").text().contains(eachValidFileTitle)) {
                                    counterDuplicated++;
                                    isDuplicated = true;
                                }
                            }
                            if (!isBanned && !isDuplicated) {
                                validFilmTitles.add(eachFilmTitle.select("font").text());
                                validFilmSubURLs.add(eachFilmTitle.select("a").first().attr("href"));
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    System.out.println("\n[x] Internal error: MalformedURL");
                } catch (IOException e) {
                    System.out.println("\n[x] An error occurred when trying to read the page");
                }
            }
        }
        System.out.println("OK");
        System.out.println("[o] \t"+counterBanned+" films banned");
        System.out.println("[o] \t"+counterDuplicated+" films dropped due to duplication");
        parseFilmPage();
    }

    void parseFilmPage() {
        System.out.println("[o] You are using favourite definition: "+configLoader.getDefinition());
        int i = 1;
        for (String each : validFilmSubURLs) {
            try {
                String targetBtFileLinkSuffix = null;
                URL url = new URL(HOST+each);
                Document document = Jsoup.parse(url, 5000);
                Elements btFileLinks = document.select("div[class=\"tinfo\"]");
                for (Element eachBtFileLink : btFileLinks) {
                    Element info = eachBtFileLink.select("span[class=\"video\"]").first();
                    if (info.text().contains(configLoader.getDefinition())) {
                        targetBtFileLinkSuffix = eachBtFileLink.select("a").first().attr("href");
                        break;
                    }
                    targetBtFileLinkSuffix = eachBtFileLink.select("a").first().attr("href");
                }
                URL targetBtFileLink = new URL(HOST+targetBtFileLinkSuffix);
                URLConnection urlConnection = targetBtFileLink.openConnection();
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream()));
                String temp;
                String arcid = null;
                String uhash = null;
                boolean hasArcid = false;
                boolean hasUhash = false;
                while ((temp = bufferedReader.readLine()) != null) {
                    if (temp.contains("var _arcid")) {
                        arcid = temp.substring(temp.indexOf("\"")+1, temp.lastIndexOf("\""));
                        hasArcid = true;
                    }
                    if (temp.contains("var _uhash")) {
                        uhash = temp.substring(temp.indexOf("\"")+1, temp.lastIndexOf("\""));
                        hasUhash = true;
                    }
                }
                if (hasArcid && hasUhash) {
                    URL requestUrl = new URL(REQUEST_URL);
                    HttpURLConnection httpUrlConnection = (HttpURLConnection) requestUrl.openConnection();
                    httpUrlConnection.setInstanceFollowRedirects(false);
                    httpUrlConnection.setDoOutput(true);
                    httpUrlConnection.setRequestMethod("POST");
                    String OUTPUT_DATA =
                            "action=download" +
                            "&id="            +
                            arcid             +
                            "&uhash="         +
                            uhash;
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                            httpUrlConnection.getOutputStream());
                    outputStreamWriter.write(OUTPUT_DATA);
                    outputStreamWriter.flush();
                    outputStreamWriter.close();
                    System.out.print("\r");
                    System.out.print("[o] Downloading torrent files...("+i+"/"+validFilmSubURLs.size()+")");
                    File file = new File(uhash+".torrent");
                    InputStream inputStream = httpUrlConnection.getInputStream();
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, length);
                        fileOutputStream.flush();
                    }
                    fileOutputStream.close();
                    httpUrlConnection.disconnect();
                }
                else 
                    System.out.println("\n[x] No download certificate spotted");
            } catch (MalformedURLException e) {
                System.out.println("\n[x] Internal error: MalformedURL");
            } catch (IOException e) {
                System.out.println("\n[x] An error occurred when trying to read the page");
            }
            i++;
        }
        System.out.println();
        System.out.println("[o] All works completed\n");
    }
}

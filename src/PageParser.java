import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

class PageParser {
    private static final String HOST = "http://www.bttiantang.com";
    private ConfigLoader configLoader = ConfigLoader.getInstance();
    private static ArrayList<String> targetCategoriesSubURLs = new ArrayList<>();
    private static ArrayList<String> validFilmTitles = new ArrayList<>();
    private static ArrayList<String> validFileSubURLs = new ArrayList<>();
    PageParser() {
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
            System.out.println("\n[x] An error occurred while trying to read the page");
            System.exit(0);
        }
        parseCategoryPage();
    }

    void parseCategoryPage() {
        System.out.println("[o] You are banning films from "+configLoader.getRegions_banned().toString());
        System.out.print("[o] Collecting films into each category...");
        int counterDuplicated = 0;
        int counterBanned = 0;
        for (String each : targetCategoriesSubURLs) {
            try {
                URL url = new URL(HOST+each);
                Document document = Jsoup.parse(url, 5000);
                Elements filmTitles = document.select("div[class=\"title\"]");
                for (Element eachFilmTitle : filmTitles) {
                    if (!"".equals(eachFilmTitle.select("font").text())) {
                        boolean isBanned = false;
                        boolean isDuplicated = false;
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
//                            System.out.println(eachFilmTitle.select("font").text());
                            validFilmTitles.add(eachFilmTitle.select("font").text());
                            validFileSubURLs.add(eachFilmTitle.select("a").first().attr("href"));
                        }
                    }
                }
            } catch (MalformedURLException e) {
                System.out.println("\n[x] Internal error: MalformedURL");
            } catch (IOException e) {
                System.out.println("\n[x] An error occurred while trying to read the page");
            }
        }
        System.out.println("OK");
        System.out.println("[o] \t"+counterBanned+" films banned");
        System.out.println("[o] \t"+counterDuplicated+" films dropped due to duplication");
    }

}

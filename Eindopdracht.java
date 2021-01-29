/**
  Naam: Luc Hengeveld
  Klas: BIN-2d
  Studentnummer: 627071
  Datum: 29-1-2021
  Toets: BI6a-O
 */

// Alles importeren:
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;


public class Eindopdracht {
    // Alle variabelen aanmaken en initialiseren
    private static final Logger logger = Logger.getLogger(Eindopdracht.class.getName());

    public static ArrayList<String> md5regels = new ArrayList<>();
    public static ArrayList<String> arraybestand1 = new ArrayList<>();
    public static ArrayList<String> arraybestand2 = new ArrayList<>();

    public static HashMap<String, HashMap<String, String>> bestandHash = new HashMap<>();

    public static HashMap<String, ArrayList<String>> rsIDbestand1 = new HashMap<>();
    public static HashMap<String, ArrayList<String>> rsIDbestand2 = new HashMap<>();
    public static HashMap<String, String[]> rsIDovereen = new HashMap<>();

    public static String MD5string, bestand, decompressedfile, bestandslocatie1, bestandslocatie2, chromosoomAndMe,
            positieAndMe, ouder1NT, ouder2NT, bestandNT;


    /**
     * Main functie
     * Download het MD5bestand, update variant_summary bestand zo nodig.
     * Vergelijkt de andme files met elkaar en vervolgens ook met de variant_summary bestand.
     * Schrijft uiteindelijk mogelijke ziektes weg in een nieuw tekstbestand.
     */
    public static void main(String[] args) throws IOException {

        // MD5 bestand downloaden
        System.out.println("Nieuw MD5 bestand downloaden...");
        MD5string = Paths.get(System.getProperty("user.home"), "Downloads\\variant_summary.txt.gz.md5").
                toString();
        InputStream inmd5 = new URL(
                "ftp://ftp.ncbi.nlm.nih.gov/pub/clinvar/tab_delimited/variant_summary.txt.gz.md5").openStream();
        Files.copy(inmd5, Paths.get(MD5string), StandardCopyOption.REPLACE_EXISTING);
        File md5bestand = new File(MD5string);

        // Inlezen van MD5 bestand en de hash code opslaan in een string
        Scanner bestandmd5lezer = new Scanner(md5bestand);
        while (bestandmd5lezer.hasNextLine()) {
            String regel = bestandmd5lezer.nextLine();
            md5regels.add(regel);
        }
        String MD5Regel = md5regels.get(0).split(" ")[0];

        // Filechooser om zelf het gz bestand in te voeren.
        // Als je het gz bestand niet hebt, vul
        System.out.println("Voer het .gz bestand in:\n");
        JFileChooser fc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        int returnValue = fc.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fc.getSelectedFile();
            bestand = selectedFile.getAbsolutePath();
        } else {
            bestand = Paths.get(System.getProperty("user.home"), "Downloads\\variant_summary.txt.gz").toString();
        }
        // Printen van de MD5 van het MD5 bestand en de MD5 van het bestand berekenen en printen.
        // Het berekenen van de MD5 gebeurd met de funcie checkSum waarbij de pathway van het gz bestand mee wordt
        // gegeven als een string.
        System.out.println("MD5     - " + MD5Regel);
        System.out.println("Bestand - " + checkSum(bestand));

        // Vergelijkt de hash code uit het MD5 bestand met de hash code van het ingevoerde .gz bestand.
        // Als het ongelijk is, wordt het nieuwe bestand gedownload en opgeslagen in downloads. Vervolgens
        // word opnieuw de hash codes vergeleken om te kijken of de download goed is gegaan.
        while (!MD5Regel.equals(checkSum(bestand))) {
            bestand = Paths.get(System.getProperty("user.home"), "Downloads\\variant_summary.txt.gz").toString();
            System.out.println("MD5 klopt niet, nieuw .gz bestand downloaden naar downloads...\n");
            InputStream in = new URL("ftp://ftp.ncbi.nlm.nih.gov/pub/clinvar/tab_delimited/variant_summary.txt.gz")
                    .openStream();
            Files.copy(in, Paths.get(bestand), StandardCopyOption.REPLACE_EXISTING);

            // Printen van de MD5 van het MD5 bestand en de MD5 van het gz bestand.
            System.out.println("MD5     - " + MD5Regel);
            System.out.println("Bestand - " + checkSum(bestand));
        }
        // Printen als MD5 bij het MD5 bestand gelijk is aan de MD5 van het .gz bestand.
        System.out.println("MD5 klopt.\n");

        // Decompressed het .gz bestand met de functie decompressGz waarbij de path naar het bestand dat gedecompressed
        // moet worden mee wordt gegeven als string.
        System.out.println("Beginnen met het .gz bestand te decompressen...");
        File bestandgz = new File(decompressGz(bestand));
        System.out.println("Decompressen klaar.\nBeginnen met inlezen en opslaan van het bestand in een HashMap...");

        // Leest het gedecompressed bestand in regel voor regel en haalt hier het chromosoom nummer, positie en NT uit.
        // Deze worden vervolgens toegevoegd aan een hashmap.
        // Deze hashmap heeft de structuur: key chromosoom (key positie (nucleotiden)).
        Scanner bestandgzlezer = new Scanner(bestandgz);
        while (bestandgzlezer.hasNextLine()) {
            String regel = bestandgzlezer.nextLine();
            String[] splitRegel = regel.split("\t");
            String bestandChr = splitRegel[18];
            String bestandPos = splitRegel[31];
            String bestandNT = splitRegel[32] + splitRegel[33];
            if (bestandHash.containsKey(bestandChr)) {
                bestandHash.get(bestandChr).put(bestandPos, bestandNT);
            } else {
                HashMap<String, String> posHash = new HashMap<>();
                posHash.put(bestandPos, bestandNT);
                bestandHash.put(bestandChr, posHash);
            }
        }
        // De eerste regel uit het bestand bevat de kolomnamen. Deze word hieronder eruitgehaald.
        bestandHash.remove("Chromosome");
        // Als het inlezen en opslaan in een HashMap klaar is, wordt dit geprint.
        System.out.println("Inlezen en opslaan HashMap klaar.\n");

        // Vraagt de gebruiker of je voorgeprogrammeerde andme bestanden wilt gebruiken of zelf 2 andme bestanden in
        // wilt voeren.
        int a = 0;
        while (a == 0) {
            Scanner andme = new Scanner(System.in);
            System.out.println("Voorgeprogrammeerde andme bestanden (1) of zelf andme bestanden invoeren (2)");
            String andmebestanden = andme.nextLine();
            if (andmebestanden.equals("1")) {
                a = 1;
            } else if (andmebestanden.equals("2")) {
                a = 2;
            } else {
                System.out.println("Alleen mogelijk om 1 of 2 in te vullen.");
            }
        }

        int x = 0;
        while (x == 0) {

            // Als er gekozen is om zelf een andme bestand in te voeren opent er een filechooser waar je het eerste
            // andme bestand kan invoeren.
            if (a == 2) {
                System.out.println("Voer het eerste andme bestand in.");
                JFileChooser fc1 = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                int returnValue1 = fc1.showOpenDialog(null);
                if (returnValue1 == JFileChooser.APPROVE_OPTION) {
                    File selectedFile1 = fc1.getSelectedFile();
                    bestandslocatie1 = selectedFile1.getAbsolutePath();
                }
            } else {
                // Als er gekozen is om voorgeprogrammeerde andme bestanden in te voeren, wordt het andme bestand
                // van Abby gedownload en opgeslagen in downloads.
                System.out.println("Het andme bestand van Abby ophalen van https://opensnp.org/users/10074");
                bestandslocatie1 = Paths.get(System.getProperty("user.home"), "Downloads\\10074.23andme.8335").
                        toString();
                InputStream inandme1 = new URL("https://opensnp.org/data/10074.23andme.8335?1611375678").
                        openStream();
                Files.copy(inandme1, Paths.get(bestandslocatie1), StandardCopyOption.REPLACE_EXISTING);
            }
            // Als de bestandslocatie "andme" bevat, wordt het bestand ingelezen en de regels opgeslagen in een
            // arraylist.
            if (bestandslocatie1.contains("andme")) {
                x = 1;
                File bestand1 = new File(bestandslocatie1);
                Scanner bestandlezer1 = null;
                try {
                    bestandlezer1 = new Scanner(bestand1);
                } catch (FileNotFoundException e) {
                    System.out.println("Bestand is niet gevonden. Voer een nieuw bestand in.");
                }
                while (true) {
                    assert bestandlezer1 != null;
                    if (!bestandlezer1.hasNextLine()) break;
                    String regel1 = bestandlezer1.nextLine();
                    if (!regel1.contains("#")) {
                        arraybestand1.add(regel1);
                    }
                }
            } else {
                // Als de bestandslocatie niet "andme" bevat, wordt er geprint dat het alleen mogelijk is om andme
                // bestanden in te voeren. Vervolgens start de while loop opnieuw zodat een nieuw andme bestand kan
                //worden gekozen in een filechooser.
                System.out.println("Alleen mogelijk om andme bestanden in te voeren.");
            }
            // Print de bestandslocatie van het eerste andme bestand.
            System.out.println(bestandslocatie1 + "\n");
        }

        int y = 0;
        while (y == 0) {

            // Als er gekozen is om zelf een andme bestand in te voeren opent er een filechooser waar je het tweede
            // andme bestand kan invoeren.
            if (a == 2) {
                System.out.println("Voer het tweede andme bestand in.");
                JFileChooser fc2 = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                int returnValue2 = fc2.showOpenDialog(null);
                if (returnValue2 == JFileChooser.APPROVE_OPTION) {
                    File selectedFile2 = fc2.getSelectedFile();
                    bestandslocatie2 = selectedFile2.getAbsolutePath();
                }
            } else {
                // Als er gekozen is om voorgeprogrammeerde andme bestanden in te voeren, wordt het andme bestand
                // van Fcousa gedownload en opgeslagen in downloads.
                System.out.println("Het andme bestand van Fcousa ophalen van https://opensnp.org/users/10068");
                bestandslocatie2 = Paths.get(System.getProperty("user.home"), "Downloads\\10068.23andme.8328").
                        toString();
                InputStream inandme2 = new URL("https://opensnp.org/data/10068.23andme.8328?1611271909").
                        openStream();
                Files.copy(inandme2, Paths.get(bestandslocatie2), StandardCopyOption.REPLACE_EXISTING);
            }
            if (bestandslocatie2.contains("andme")) {
                // Als de bestandslocatie "andme" bevat, wordt het bestand ingelezen en de regels opgeslagen in een
                // arraylist.
                y = 1;
                File bestand2 = new File(bestandslocatie2);
                Scanner bestandlezer2 = null;
                try {
                    bestandlezer2 = new Scanner(bestand2);
                } catch (FileNotFoundException e) {
                    System.out.println("Bestand is niet gevonden. Voer een nieuw bestand in.");
                }
                while (true) {
                    assert bestandlezer2 != null;
                    if (!bestandlezer2.hasNextLine()) break;
                    String regel2 = bestandlezer2.nextLine();
                    if (!regel2.contains("#")) {
                        arraybestand2.add(regel2);
                    }
                }
            } else {
                // Als de bestandslocatie niet "andme" bevat, wordt er geprint dat het alleen mogelijk is om andme
                // bestanden in te voeren. Vervolgens start de while loop opnieuw zodat een nieuw andme bestand kan
                //worden gekozen in een filechooser.
                System.out.println("Alleen mogelijk om andme bestanden in te voeren.");
            }
            // Print de bestandslocatie van het tweede andme bestand.
            System.out.println(bestandslocatie2 + "\n");
        }

        // Begint met het toevoegen van de gegevens van het eerste andme bestand aan een tijdelijke array en voegt het
        // vervolgens toe aan een HashMap.
        // De Hashmap heeft de structuur key: rsID (Values: Chromosoom, Positie, Genotype)
        System.out.println("Andme bestand 1 toevoegen aan een tijdelijke ArrayList en een hashmap...");
        for (String value : arraybestand1) {
            ArrayList<String> tempArray1 = new ArrayList<>();
            tempArray1.add(Arrays.toString(new String[]{value.split("\t")[1]}).
                    replaceAll("[\\[\\]]", ""));
            tempArray1.add(Arrays.toString(new String[]{value.split("\t")[2]}).
                    replaceAll("[\\[\\]]", ""));
            tempArray1.add(Arrays.toString(new String[]{value.split("\t")[3]}).
                    replaceAll("[\\[\\]]", ""));
            rsIDbestand1.put(Arrays.toString(new String[]{value.split("\t")[0]}).
                    replaceAll("[\\[\\]]", ""), tempArray1);
        }

        // Begint met het toevoegen van de gegevens van het tweede andme bestand aan een tijdelijke array en voegt het
        // vervolgens toe aan een HashMap.
        // De Hashmap heeft de structuur key: rsID (Values: Chromosoom, Positie, Genotype)
        System.out.println("Andme bestand 2 toevoegen aan een tijdelijke ArrayList en een hashmap... \n");
        for (String s : arraybestand2) {
            ArrayList<String> tempArray2 = new ArrayList<>();
            tempArray2.add(Arrays.toString(new String[]{s.split("\t")[1]}).
                    replaceAll("[\\[\\]]", ""));
            tempArray2.add(Arrays.toString(new String[]{s.split("\t")[2]}).
                    replaceAll("[\\[\\]]", ""));
            tempArray2.add(Arrays.toString(new String[]{s.split("\t")[3]}).
                    replaceAll("[\\[\\]]", ""));
            rsIDbestand2.put(Arrays.toString(new String[]{s.split("\t")[0]}).
                    replaceAll("[\\[\\]]", ""), tempArray2);
        }

        // Loopt door de keys van het eerste rsID hashmap. Als hashmap rsID 1 en hashmap rsID 2 allebei dezelfde key
        // bevatten, dan worden de values toegevoegd aan een string array en vervolgens toegevoegd aan een hashmap
        // genaamd rsIDOvereen met de structuur key: rsID (Value: [Chromosoom, Positie, NT ouder1, NT ouder2])
        for (String i : rsIDbestand1.keySet()) {
            if (rsIDbestand2.containsKey(i)) {
                String[] overeen = (rsIDbestand1.get(i).toString() + ", " + rsIDbestand2.get(i).get(2)).
                        replaceAll("[\\[\\]]", "").split(", ");
                rsIDovereen.put(i, overeen);
            }
        }

        // Begint met het aanmaken van een path naar downloads. Het bestand waar de gegevens uiteindelijk in worden
        // weggeschreven heet "EindOpdrachtZiektesLuc".
        System.out.println("Mogelijke ziektes worden nu geschreven naar het bestand \"EindOpdrachtZiektesLuc\" " +
                "bij downloads...");
        String bestandslocatie3 = Paths.get(System.getProperty("user.home"), "Downloads\\" +
                "EindOpdrachtZiektesLuc.txt").toString();
        boolean nieuwbestand = true;

        // Loopt door de rsIDovereen hashmap en haalt hier het chromosoomnummer, positie, NT ouder1 en NT ouder2 uit.
        // Deze gegevens worden opgeslagen in een string variabele.
        for (String i : rsIDovereen.keySet()) {
            chromosoomAndMe = rsIDovereen.get(i)[0];
            positieAndMe = rsIDovereen.get(i)[1];
            ouder1NT = rsIDovereen.get(i)[2];
            ouder2NT = rsIDovereen.get(i)[3];
            // Zoekt chromosoomnummer op in de variant summary hashmap. Vervolgens word hierin de key positie opgezocht
            // Voor de nucleotiden op te halen in een bepaald chromosoom, op een bepaalde positie.
            try {
                bestandNT = bestandHash.get(chromosoomAndMe).get(positieAndMe);

                // Als de genotype van ouder1 of ouder2 gelijk is aan de nucleotiden uit de variant summary hashmap,
                // dan wordt het ouder ID uit de bestandslocatie van de andme bestanden gehaald.
                // De voorste getallen worden hierbij gepakt, dus bij bijvoorbeeld 10074.23andme.8335 wordt als ouder ID
                // 10074 gebruikt
                if (ouder1NT.equals(bestandNT) || ouder2NT.equals(bestandNT)) {
                    String[] ouder1Split = bestandslocatie1.split("\\\\");
                    String ouder1ID = ouder1Split[ouder1Split.length-1].split("\\.")[0];

                    String[] ouder2Split = bestandslocatie2.split("\\\\");
                    String ouder2ID = ouder2Split[ouder2Split.length-1].split("\\.")[0];

                    // Als het een nieuw bestand is, wordt eerst bovenaan de kolomnamen toegevoegd. Vervolgens worden de
                    // bijbehorende waardes hiervan toegevoegd.
                    if (nieuwbestand) {
                        Writer ziektes = new FileWriter(bestandslocatie3, false);
                        ziektes.write("#rsID\tNT_combinatie\tChromosoom\tNT_ouder1\tNT_ouder2\tID_ouder1\t" +
                                "ID_ouder2\n");
                        ziektes.write(i + "\t" + bestandNT + "\t" + chromosoomAndMe + "\t" + ouder1NT + "\t" +
                                ouder2NT + "\t" + ouder1ID + "\t" + ouder2ID + "\n");
                        ziektes.flush();
                        ziektes.close();
                        nieuwbestand = false;


                    } else {
                        // Als het geen nieuw bestand is, worden alleen nieuwe waardes onderaan toegevoegd (append).
                        Writer ziektes = new FileWriter(bestandslocatie3, true);
                        ziektes.write(i + "\t" + bestandNT + "\t" + chromosoomAndMe + "\t" + ouder1NT + "\t" +
                                ouder2NT + "\t" + ouder1ID + "\t" + ouder2ID + "\n");
                        ziektes.flush();
                        ziektes.close();
                    }
                }
                // Als de key niet kan worden gevonden in de variant summary hashmap, wordt deze overgeslagen met de
                // catch hieronder.
            } catch (NullPointerException ignored) {}
        }
        // Print als alle code klaar is met runnen.
        System.out.println("Code klaar met runnen.");
    }

    /**
     * Functie voor het berekenen van de hashcode.
     * Parameter: locatie van het bestand in een string.
     * Return: String met de hashcode van het bestand.
     */
    public static String checkSum(String path){
        String checksum = null;
        try {
            FileInputStream fis = new FileInputStream(path);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int numOfBytesRead;
            while( (numOfBytesRead = fis.read(buffer)) > 0) {
                md.update(buffer, 0, numOfBytesRead);
            }
            byte[] hash = md.digest();
            checksum = new BigInteger(1, hash).toString(16); //don't use this, truncates leading zero
        } catch (IOException | NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return checksum;
    }

    /**
     * Functie voor het decompressen van een .gz bestand. Slaat het gedecompresseerde bestand op in downloads.
     * Parameter: locatie van het .gz bestand in een string.
     * Return: String met de locatie van het gedecompresseerde bestand.
     *
     * Opmerking: Het is alleen mogelijk om andme bestanden in te voeren bij het andme gedeelte. Illumina bestanden
     * worden niet geaccepteerd. Het print als een verkeerd bestand is ingevoerd en vraagt opnieuw voor een andme
     * bestand.
     */
    public static String decompressGz(String gzbestand) {
        // Locatie van de output.
        String targetFile = Paths.get(System.getProperty("user.home"), "Downloads\\variant_summary.txt").
                toString();

        // Aanmaken van try catch met de catch IOException
        try (
            // Maakt een filestream aan voor het inlezen van het gzbestand.
            FileInputStream fis = new FileInputStream(gzbestand);

            // Maakt een filestream aan voor het decompresseren van de filestream van het gzbestand
            GZIPInputStream gzis = new GZIPInputStream(fis);

            // Aanmaak van de output stream en slaat de gegevens op in het bestand van de output locatie.
            FileOutputStream fos = new FileOutputStream(targetFile)
            ) {
            // Maakt een buffer aan en een tijdelijke variable voor het decompressen.
            byte[] buffer = new byte[1024];
            int length;

            // Leest de compressed bestand en schrijft vervolgens de gedecompresseerde informatie in het output bestand.
            while ((length = gzis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            // Gooit de IOException als het niet gelukt is om het bestand weg te schrijven.
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Slaat de path van het outputbestand op in een variable en returned deze, zodat dit gebruikt kan worden in
        // de rest van de code.
        decompressedfile = Paths.get(System.getProperty("user.home"), "Downloads\\variant_summary.txt").
                toString();
        return decompressedfile;
    }
}

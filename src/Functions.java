import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Functions{

    public static String name() {
        return "Functions";
    }

    public static boolean checkFile(File file){
        try{
            BufferedReader reader = new BufferedReader(new FileReader(file));
            reader.readLine();
            return true;
        }catch(NullPointerException e){
            Functions.printErr(name(), "File not found " + file.getName());
        }catch(FileNotFoundException e){
            Functions.printErr(name(), "Error opening file " + file.getName());
        }catch (IOException e){
            System.out.println("Functions_checkFile: Sudden end. " + file.getName());
        }
        return false;
    }

    public static String getMasterIP(String config_file){
        String masterIP = null;
        try{
            Stream<String> file = Files.lines(Paths.get(config_file));
            masterIP = file.filter(s -> s.trim().startsWith("masterIP")).map(s -> s.trim().substring(s.indexOf(" ")).trim()).findFirst().get();
        }catch(IOException e){
            Functions.printErr(name(), "Functions_getMasterIP: IO Error");
        }
        return masterIP;
    }

    public static int getMasterPort(String config_file){
        String masterPort = "";
        try{
            Stream<String> file = Files.lines(Paths.get(config_file));
            masterPort = file.filter(s -> s.trim().startsWith("masterPort")).map(s -> s.trim().substring(s.indexOf(" ")).trim()).findFirst().get();
        }catch(IOException e){
            Functions.printErr(name(), "Functions_getMasterIP: IO Error");
        }
        return Integer.parseInt(masterPort);
    }

    public static void setReducer(String ip, String port, String config_file){
        try{
            Stream<String> file = Files.lines(Paths.get(config_file));
            List<String> lines = file.collect(Collectors.toList());
            lines.removeIf(s -> s.startsWith("reducerIP"));
            lines.add("reducerIP " + ip);
            lines.removeIf(s -> s.startsWith("reducerPort"));
            lines.add("reducerPort " + port);

            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(config_file)));
            for(String s : lines){
                writer.write(s);
                writer.newLine();
                writer.flush();
            }
        }catch(IOException e){
            Functions.printErr(name(), "Functions_getMasterIP: IO Error");
        }
    }

    public static String getReducerIP(String config_file){
        String reducerIP = null;
        try{
            Stream<String> file = Files.lines(Paths.get(config_file));
            reducerIP = file.filter(s -> s.trim().startsWith("reducerIP")).map(s -> s.trim().substring(s.indexOf(" ")).trim()).findFirst().get();
        }catch(IOException e){
            Functions.printErr(name(), "Functions_getMasterIP: IO Error");
        }
        return reducerIP;
    }

    public static int getReducerPort(String config_file){
        String reducerPort = "0";
        try{
            Stream<String> file = Files.lines(Paths.get(config_file));
            reducerPort = file.filter(s -> s.trim().startsWith("reducerPort")).map(s -> s.trim().substring(s.indexOf(" ")).trim()).findFirst().get();
        }catch(IOException e){
            Functions.printErr(name(), "Functions_getReducerPort: IO Error");
        }
        return Integer.parseInt(reducerPort);
    }

    public static String getMyIP(String config_file){
        String myIP = null;
        try{
            Stream<String> file = Files.lines(Paths.get(config_file));
            myIP = file.filter(s -> s.trim().startsWith("myIP")).map(s -> s.trim().substring(s.indexOf(" ")).trim()).findFirst().get();
        }catch(IOException e){
            Functions.printErr(name(), "Functions_getMasterIP: IO Error");
        }
        return myIP;
    }

    public static String getTime(){
        return String.format("%d:%d:%d.%d: ", LocalDateTime.now().getHour(),
                                            LocalDateTime.now().getMinute(), 
                                            LocalDateTime.now().getSecond(), 
                                            LocalDateTime.now().getNano());
    }

    public static void printErr(String tag, String error){
        String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
        System.err.println(getTime() + tag + "_" + methodName + " \"" + error + "\"");
    }

}

















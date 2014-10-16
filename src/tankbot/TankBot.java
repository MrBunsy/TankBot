/**
 * TankBot assumes that pi-blaster is installed and running on the Pi to produce
 * PWM signals
 */
package tankbot;

import jargs.gnu.CmdLineParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author Luke
 */
public class TankBot {

    public static void printUsage(){
        System.out.println("TankBot - For driving a miniture tank\n\n"
                + "Usage:\n"
                + "-s --server: Run a server, not a client\b"
                + "-c --config: Config file to use"
                + "When running as a client:\n"
                + "-p --port: Port to connect to\n"
                + "-i --ip: IP address to connect to\n"
                + "-d --debug: Print debug information\n"
                + "Note, command line arguments will override config file");
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        CmdLineParser parser = new CmdLineParser();
        
        CmdLineParser.Option serverArg = parser.addBooleanOption('s', "server");
        CmdLineParser.Option portArg = parser.addIntegerOption('p', "port");
        CmdLineParser.Option ipArg = parser.addStringOption('i',"ip");
        CmdLineParser.Option debugArg = parser.addBooleanOption('d',"debug");
        CmdLineParser.Option configFileArg = parser.addStringOption('c',"config");
        
        
        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();            
        }
        
        String propertiesFile=(String)parser.getOptionValue(configFileArg, "GunControl.ini");
        
        Properties properties= new Properties();
        
        File configFile = new File(propertiesFile);
        if(configFile.exists()){
            try {
                properties.load(new FileReader(configFile));
            } catch (IOException ex) {
                System.err.println("Failed to read config file "+ex.getMessage());
            }
        }
        properties.list(System.out);
        
        boolean defaultServer = Boolean.parseBoolean(properties.getProperty("server", "false"));
        int defaultPort = Integer.parseInt(properties.getProperty("port", "1234"));
        String defaultIp = properties.getProperty("ip", "localhost");
        
        boolean actAsServer = (Boolean) parser.getOptionValue(serverArg, defaultServer);
        int port = (Integer) parser.getOptionValue(portArg, defaultPort);
        String ip = (String)parser.getOptionValue(ipArg, defaultIp);
        boolean debug = (Boolean) parser.getOptionValue(debugArg,false);
        
        if(actAsServer){
            
            Server server = new Server(port, debug);
            
        }else{
            Client client = new Client(properties, debug);
//            MotorState motorState = new MotorState(client.getJoystick(), properties);
            ControlWindow window = new ControlWindow(client, client.getMotorState(), ip, port);
            window.setVisible(true);
            
            client.connectTo(ip, port);
            
        }
    }

}

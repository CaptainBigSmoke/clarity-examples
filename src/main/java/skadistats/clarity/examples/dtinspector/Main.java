package skadistats.clarity.examples.dtinspector;

import java.awt.EventQueue;

import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeModel;

import skadistats.clarity.Clarity;
import skadistats.clarity.match.Match;
import skadistats.clarity.parser.DemoInputStreamIterator;
import skadistats.clarity.parser.Profile;

public class Main {

    public static void main(String[] args) throws Exception {

        final Match match = new Match();
        DemoInputStreamIterator iter = Clarity.iteratorForFile(args[0], Profile.SEND_TABLES);
        while(iter.hasNext()) {
            iter.next().apply(match);
        }
        
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());        
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainWindow window = new MainWindow();
                    window.getClassTree().setModel(new DefaultTreeModel(new TreeConstructor(match).construct()));
                    window.getFrame().setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }
    
}

/*
 * AntiSpoiler tool, written by CountableFiber @ reddit
 *
 * This product includes software developed by skadistats (http://www.skadistats.com/).
 */
package main;

import com.dota2.proto.Demo.CDemoFileInfo;
import com.google.common.io.Files;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;
import skadistats.clarity.Clarity;

public class AntiSpoiler {

    public static void main(String[] args) throws Exception {
        Logger log = LoggerFactory.getLogger("simple");

        if (args.length <= 0) {
            log.info("Usage: antispoiler.jar <directory or replay file>.");
            return;
        }

        String replay_file_name = args[0];
        String backup_file_name;
        if(replay_file_name.endsWith("\\")) {
            replay_file_name = replay_file_name.substring(0,replay_file_name.length()-1);
        }
        if(replay_file_name.endsWith("\"")) { //Windows hack..
            replay_file_name = replay_file_name.substring(0,replay_file_name.length()-1);
        }

        File test_read = new File(replay_file_name);
        if (!test_read.exists()) {
            log.error("The file/directory '" + replay_file_name + "' does not exist.");
            return;
        }
        if (test_read.isFile()) {
            try {
                File already_converted = new File(replay_file_name + ".back");
                if (already_converted.exists()) {
                    log.error("The replay '" + replay_file_name + "' was already converted.");
                    return;
                }
                File demo_file = new File(replay_file_name);
                File backup = new File(demo_file.getParent(), demo_file.getName() + ".back");
                Files.move(demo_file, backup);
                backup_file_name = replay_file_name + ".back";
                ModifyDemo(backup_file_name, replay_file_name);
                log.info("The replay '" + replay_file_name + "' was successfully converted.");
            } catch (IOException e) {
                log.error("The replay '" + replay_file_name + "' could not be opened or does not exist or is too old to be converted.");
                return;
            }
        } else {
            if (test_read.isDirectory()) {
                File[] files = test_read.listFiles();
                for (File file : files) {
                    if (file.getName().endsWith("dem") && file.isFile()) {
                        try {
                            replay_file_name = file.getName();
                            File already_converted = new File(file.getAbsoluteFile() + ".back");
                            if (already_converted.exists()) {
                                continue;
                            }
                            File demo_file = file;
                            File backup = new File(demo_file.getParent(), demo_file.getName() + ".back");
                            Files.move(demo_file, backup);
                            backup_file_name = replay_file_name + ".back";
                            ModifyDemo(backup.getAbsolutePath(), demo_file.getAbsolutePath());
                            log.info("The replay '" + replay_file_name + "' was successfully converted.");
                        } catch (IOException e) {
                            log.info("(Warning) The file '" + replay_file_name + "' could not be opened or does not exist or is too old to be converted.");
                        }
                    }
                }
            }
        }
    }

    private static void ModifyDemo(String original, String replication) throws Exception {
        Logger log = LoggerFactory.getLogger("simple");
        CDemoFileInfo info;
        try {
            info = Clarity.infoForFile(original);
        } catch (IOException e) {
            log.error("The replay '" + original + "' could not be opened or does not exist.");
            return;
        }
        CodedInputStream orig_rep = CodedInputStream.newInstance(new FileInputStream(original));
        CodedOutputStream output_coded = CodedOutputStream.newInstance(new FileOutputStream(replication));
        orig_rep.setSizeLimit(Integer.MAX_VALUE);
        
        String header = new String(orig_rep.readRawBytes(8));
        int offset = orig_rep.readFixed32();
        orig_rep.skipRawBytes(offset - 12);
        int flags = orig_rep.readRawVarint32();
        int peek_tick = orig_rep.readRawVarint32(); // skip peek tick
        int pay_size = orig_rep.readRawVarint32();
        orig_rep = CodedInputStream.newInstance(new FileInputStream(original));
        byte[] prequel = orig_rep.readRawBytes(offset);

        //Modify info to your liking now
        java.lang.reflect.Field playbackTickField = CDemoFileInfo.class.getDeclaredField("playbackTicks_"); //dirty hack
        playbackTickField.setAccessible(true);
        playbackTickField.set(info, 30 * 120 * 60);

        byte[] serialized = info.toByteArray();
        byte[] compressed = Snappy.compress(serialized);

        output_coded.writeRawBytes(prequel);
        output_coded.writeRawVarint32(flags);
        output_coded.writeRawVarint32(peek_tick);
        output_coded.writeRawVarint32(compressed.length);
        output_coded.writeRawBytes(compressed);
        output_coded.flush();
    }

}

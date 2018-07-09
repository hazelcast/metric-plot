package com.hazelcast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Metrics {

    private final MetricsCli cli;

    public Metrics(String[] args) {
        cli = new MetricsCli(args);
    }

    public static void main(String[] args) throws IOException {
        for (String arg : args) {
            System.out.println(arg);
        }
        System.out.println("--------");

        Metrics metrics = new Metrics(args);
        metrics.render();

        // write your code here
    }

    private void render() throws IOException {
        Map<String, StringBuffer> result = generate(cli.inputFile());

        StringBuffer data = new StringBuffer();
        StringBuffer select = new StringBuffer();
        StringBuffer selectHandler = new StringBuffer();

        List<String> keys = new ArrayList<>(result.size());
        Map<String, StringBuffer> sanitizedResult = new HashMap<>(result.size());
        Map<String, String> keyMapping = new HashMap<>();
        for (String key : result.keySet()) {
            String sanitizedKey = sanitize(key);
            keyMapping.put(sanitizedKey, key);
            keys.add(sanitizedKey);
            sanitizedResult.put(sanitizedKey, result.remove(key));
        }

        result = sanitizedResult;

        Collections.sort(keys);

        for (String name : keys) {

            data.append("var data_" + name + " = google.visualization.arrayToDataTable([\n");
            data.append("['X', 'Y'],\n");
            data.append(result.get(name));
            data.append("]);\n");

            select.append("   <option value=\"data_" + name + "\">" + escapeHTML(keyMapping.get(name)) + "</option>\n");

            selectHandler.append("          if(selValue=='data_" + name + "'){x=data_" + name + ";}\n");
        }

        writeResult(data, select, selectHandler);
    }


    public static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String sanitize(String name) {
        return name
                .replace("/", "_")
                .replace(">", "_")
                .replace(".", "_")
                .replace("[", "_")
                .replace("]", "_")
                .replace("-", "_")
                .replace(":", "_");
    }

    private void writeResult(StringBuffer data, StringBuffer select, StringBuffer selectHandler) throws IOException {
        String template = loadTemplate();
        String result = template.replace("$data", data);
        result = result.replace("$select", select);
        result = result.replace("$handler", selectHandler);
        try (PrintStream out = new PrintStream(new FileOutputStream(cli.outputFile()))) {
            out.print(result);
        }
    }

    private Map<String, StringBuffer> generate(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();

        Map<String, StringBuffer> map = new ConcurrentHashMap<>();

        while (line != null) {
            if (line.contains("Metric")) {


//                if (line.contains(metric)) {
//                    lineCount++;
//                    if(lineCount>5){
//                        break;
//                    }
                String[] split = line.split(" ");


                long time = Long.parseLong(split[2]);
                if (cli.fromEpoch() <= time && cli.toEpoch() >= time) {

                    String[] metricKeyValue = split[3].split("=");
                    String metric = metricKeyValue[0].substring(7);
                    String value = metricKeyValue[1];

                    StringBuffer sb = map.computeIfAbsent(metric, k -> new StringBuffer());

                    value = value.substring(0, value.length() - 1);


                    // System.out.println(metric);

                    //if(sb.length()<100) {

                    if (sb.length() > 0) {
                        sb.append(",");
                    }

                    sb.append("[new Date(").append(time).append("),").append(value).append("]");
                    sb.append("\n");
                    //}
                }
            }
            line = reader.readLine();
        }
        return map;
    }

    private static String loadTemplate() throws IOException {
        ClassLoader classLoader = Metrics.class.getClassLoader();
        File file = new File(classLoader.getResource("template.html").getFile());
        byte[] encoded = Files.readAllBytes(file.toPath());
        return new String(encoded, StandardCharsets.UTF_8);
    }
}

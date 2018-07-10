package com.hazelcast.metricplot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

public class MetricPlot {

    public static void main(String[] args) throws IOException {
        MetricPlot metrics = new MetricPlot(args);
        metrics.render();
    }

    private final MetricsCli cli;

    public MetricPlot(String[] args) {
        cli = new MetricsCli(args);
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

        MetricsFilter filter = cli.filter();

        while (line != null) {
            if (line.contains("Metric")) {

                String[] split = line.split(" ");

                long time = Long.parseLong(split[2]);
                String[] metricKeyValue = split[3].split("=");
                String metric = metricKeyValue[0].substring(7);
                String value = metricKeyValue[1];

                if (cli.fromEpoch() <= time && cli.toEpoch() >= time && filter.test(metric)) {
                    StringBuffer sb = map.computeIfAbsent(metric, k -> new StringBuffer());

                    value = value.substring(0, value.length() - 1);

                    if (sb.length() > 0) {
                        sb.append(",");
                    }

                    sb.append("[new Date(").append(time).append("),").append(value).append("]");
                    sb.append("\n");
                }
            }
            line = reader.readLine();
        }
        return map;
    }

    private static String loadTemplate() throws IOException {
        ClassLoader classLoader = MetricPlot.class.getClassLoader();
        String name = "/template.html";
        InputStream stream = MetricPlot.class.getResourceAsStream(name);
        if(stream == null){
            throw new IOException(format("Failed to load resource '%s'",name));
        }

        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        return textBuilder.toString();
//
//        File file = new File(stream.getFile());
//        byte[] encoded = Files.readAllBytes(file.toPath());
//        return new String(encoded, StandardCharsets.UTF_8);
    }
}

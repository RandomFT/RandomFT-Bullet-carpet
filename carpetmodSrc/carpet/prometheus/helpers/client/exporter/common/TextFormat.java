package carpet.prometheus.helpers.client.exporter.common;

import carpet.prometheus.helpers.client.Collector;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class TextFormat {
    public static final String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";
    public static final String CONTENT_TYPE_OPENMETRICS_100 = "application/openmetrics-text; version=1.0.0; charset=utf-8";

    public TextFormat() {
    }

    public static String chooseContentType(String acceptHeader) {
        if (acceptHeader == null) {
            return "text/plain; version=0.0.4; charset=utf-8";
        } else {
            String[] var1 = acceptHeader.split(",");
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                String accepts = var1[var3];
                if ("application/openmetrics-text".equals(accepts.split(";")[0].trim())) {
                    return "application/openmetrics-text; version=1.0.0; charset=utf-8";
                }
            }

            return "text/plain; version=0.0.4; charset=utf-8";
        }
    }

    public static void writeFormat(String contentType, Writer writer, Enumeration<Collector.MetricFamilySamples> mfs) throws IOException {
        if ("text/plain; version=0.0.4; charset=utf-8".equals(contentType)) {
            write004(writer, mfs);
        } else if ("application/openmetrics-text; version=1.0.0; charset=utf-8".equals(contentType)) {
            writeOpenMetrics100(writer, mfs);
        } else {
            throw new IllegalArgumentException("Unknown contentType " + contentType);
        }
    }

    public static void write004(Writer writer, Enumeration<Collector.MetricFamilySamples> mfs) throws IOException {
        TreeMap omFamilies = new TreeMap();

        label72:
        while(mfs.hasMoreElements()) {
            Collector.MetricFamilySamples metricFamilySamples = (Collector.MetricFamilySamples)mfs.nextElement();
            String name = metricFamilySamples.name;
            writer.write("# HELP ");
            writer.write(name);
            if (metricFamilySamples.type == Collector.Type.COUNTER) {
                writer.write("_total");
            }

            if (metricFamilySamples.type == Collector.Type.INFO) {
                writer.write("_info");
            }

            writer.write(32);
            writeEscapedHelp(writer, metricFamilySamples.help);
            writer.write(10);
            writer.write("# TYPE ");
            writer.write(name);
            if (metricFamilySamples.type == Collector.Type.COUNTER) {
                writer.write("_total");
            }

            if (metricFamilySamples.type == Collector.Type.INFO) {
                writer.write("_info");
            }

            writer.write(32);
            writer.write(typeString(metricFamilySamples.type));
            writer.write(10);
            String createdName = name + "_created";
            String gcountName = name + "_gcount";
            String gsumName = name + "_gsum";
            Iterator var8 = metricFamilySamples.samples.iterator();

            while(true) {
                while(true) {
                    if (!var8.hasNext()) {
                        continue label72;
                    }

                    Collector.MetricFamilySamples.Sample sample = (Collector.MetricFamilySamples.Sample)var8.next();
                    if (!sample.name.equals(createdName) && !sample.name.equals(gcountName) && !sample.name.equals(gsumName)) {
                        writer.write(sample.name);
                        if (sample.labelNames.size() > 0) {
                            writer.write(123);

                            for(int i = 0; i < sample.labelNames.size(); ++i) {
                                writer.write((String)sample.labelNames.get(i));
                                writer.write("=\"");
                                writeEscapedLabelValue(writer, (String)sample.labelValues.get(i));
                                writer.write("\",");
                            }

                            writer.write(125);
                        }

                        writer.write(32);
                        writer.write(Collector.doubleToGoString(sample.value));
                        if (sample.timestampMs != null) {
                            writer.write(32);
                            writer.write(sample.timestampMs.toString());
                        }

                        writer.write(10);
                    } else {
                        Collector.MetricFamilySamples omFamily = (Collector.MetricFamilySamples)omFamilies.get(sample.name);
                        if (omFamily == null) {
                            omFamily = new Collector.MetricFamilySamples(sample.name, Collector.Type.GAUGE, metricFamilySamples.help, new ArrayList());
                            omFamilies.put(sample.name, omFamily);
                        }

                        omFamily.samples.add(sample);
                    }
                }
            }
        }

        if (!omFamilies.isEmpty()) {
            write004(writer, Collections.enumeration(omFamilies.values()));
        }

    }

    private static void writeEscapedHelp(Writer writer, String s) throws IOException {
        for(int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch(c) {
                case '\n':
                    writer.append("\\n");
                    break;
                case '\\':
                    writer.append("\\\\");
                    break;
                default:
                    writer.append(c);
            }
        }

    }

    private static void writeEscapedLabelValue(Writer writer, String s) throws IOException {
        for(int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch(c) {
                case '\n':
                    writer.append("\\n");
                    break;
                case '"':
                    writer.append("\\\"");
                    break;
                case '\\':
                    writer.append("\\\\");
                    break;
                default:
                    writer.append(c);
            }
        }

    }

    private static String typeString(Collector.Type t) {
        switch(t) {
            case GAUGE:
                return "gauge";
            case COUNTER:
                return "counter";
            case SUMMARY:
                return "summary";
            case HISTOGRAM:
                return "histogram";
            case GAUGE_HISTOGRAM:
                return "histogram";
            case STATE_SET:
                return "gauge";
            case INFO:
                return "gauge";
            default:
                return "untyped";
        }
    }

    public static void writeOpenMetrics100(Writer writer, Enumeration<Collector.MetricFamilySamples> mfs) throws IOException {
        while(mfs.hasMoreElements()) {
            Collector.MetricFamilySamples metricFamilySamples = (Collector.MetricFamilySamples)mfs.nextElement();
            String name = metricFamilySamples.name;
            writer.write("# TYPE ");
            writer.write(name);
            writer.write(32);
            writer.write(omTypeString(metricFamilySamples.type));
            writer.write(10);
            if (!metricFamilySamples.unit.isEmpty()) {
                writer.write("# UNIT ");
                writer.write(name);
                writer.write(32);
                writer.write(metricFamilySamples.unit);
                writer.write(10);
            }

            writer.write("# HELP ");
            writer.write(name);
            writer.write(32);
            writeEscapedLabelValue(writer, metricFamilySamples.help);
            writer.write(10);

            for(Iterator var4 = metricFamilySamples.samples.iterator(); var4.hasNext(); writer.write(10)) {
                Collector.MetricFamilySamples.Sample sample = (Collector.MetricFamilySamples.Sample)var4.next();
                writer.write(sample.name);
                int i;
                if (sample.labelNames.size() > 0) {
                    writer.write(123);

                    for(i = 0; i < sample.labelNames.size(); ++i) {
                        if (i > 0) {
                            writer.write(",");
                        }

                        writer.write((String)sample.labelNames.get(i));
                        writer.write("=\"");
                        writeEscapedLabelValue(writer, (String)sample.labelValues.get(i));
                        writer.write("\"");
                    }

                    writer.write(125);
                }

                writer.write(32);
                writer.write(Collector.doubleToGoString(sample.value));
                if (sample.timestampMs != null) {
                    writer.write(32);
                    omWriteTimestamp(writer, sample.timestampMs);
                }

                if (sample.exemplar != null) {
                    writer.write(" # {");

                    for(i = 0; i < sample.exemplar.getNumberOfLabels(); ++i) {
                        if (i > 0) {
                            writer.write(",");
                        }

                        writer.write(sample.exemplar.getLabelName(i));
                        writer.write("=\"");
                        writeEscapedLabelValue(writer, sample.exemplar.getLabelValue(i));
                        writer.write("\"");
                    }

                    writer.write("} ");
                    writer.write(Collector.doubleToGoString(sample.exemplar.getValue()));
                    if (sample.exemplar.getTimestampMs() != null) {
                        writer.write(32);
                        omWriteTimestamp(writer, sample.exemplar.getTimestampMs());
                    }
                }
            }
        }

        writer.write("# EOF\n");
    }

    static void omWriteTimestamp(Writer writer, long timestampMs) throws IOException {
        writer.write(Long.toString(timestampMs / 1000L));
        writer.write(".");
        long ms = timestampMs % 1000L;
        if (ms < 100L) {
            writer.write("0");
        }

        if (ms < 10L) {
            writer.write("0");
        }

        writer.write(Long.toString(timestampMs % 1000L));
    }

    private static String omTypeString(Collector.Type t) {
        switch(t) {
            case GAUGE:
                return "gauge";
            case COUNTER:
                return "counter";
            case SUMMARY:
                return "summary";
            case HISTOGRAM:
                return "histogram";
            case GAUGE_HISTOGRAM:
                return "gaugehistogram";
            case STATE_SET:
                return "stateset";
            case INFO:
                return "info";
            default:
                return "unknown";
        }
    }
}

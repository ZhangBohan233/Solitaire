package trashsoftware.solitaire.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SolitaireRecorder {

    public static final String RECORD_FILE_NAME = "records.json";
    public static final DecimalFormat DECIMAL_FORMAT = (DecimalFormat) DecimalFormat.getNumberInstance();
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd,HH-mm-ss");
    private static final Record record = loadRecord();

    /**
     * Records a game result and returns its ranking and the previous best records.
     *
     * @param solitaireRecord the result to be recorded
     * @return the ranking of this and previous bests
     */
    public static SolitaireRankResult put(SolitaireRecord solitaireRecord) {
        SolitaireRankResult scoreTimeSteps = record.put(solitaireRecord);
        saveRecord();
        return scoreTimeSteps;
    }

    private static Record loadRecord() {
        try (BufferedReader br = new BufferedReader(new FileReader(RECORD_FILE_NAME))) {
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = br.readLine()) != null) builder.append(line);
            return new Record(new JSONObject(builder.toString()));
        } catch (FileNotFoundException e) {
            return new Record();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveRecord() {
        try (FileWriter fw = new FileWriter(RECORD_FILE_NAME)) {
            String recordedString = record.toJson().toString(2);
            fw.write(recordedString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Record {
        private final Map<Integer, LevelRecord> levelMap = new TreeMap<>();

        Record() {
            this(new JSONObject());
        }

        Record(JSONObject jsonObject) {
            for (String key : jsonObject.keySet()) {
                try {
                    int level = Integer.parseInt(key);
                    JSONArray array = jsonObject.getJSONArray(key);
                    LevelRecord levelRecord = LevelRecord.fromJson(array);
                    levelMap.put(level, levelRecord);
                } catch (JSONException | NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            for (Map.Entry<Integer, LevelRecord> entry : levelMap.entrySet()) {
                object.put(String.valueOf(entry.getKey()), entry.getValue().toJson());
            }

            return object;
        }

        SolitaireRankResult put(SolitaireRecord solitaireRecord) {
            LevelRecord levelRecord = levelMap.get(solitaireRecord.initFinish);
            if (levelRecord == null) {
                levelMap.put(solitaireRecord.initFinish, new LevelRecord(solitaireRecord));
                return new SolitaireRankResult(
                        0, null,
                        0, null,
                        0, null
                );  // all 0
            }
            return levelRecord.insert(solitaireRecord);
        }
    }

    private static class LevelRecord {
        private final SortedList<SolitaireRecord> scoreList;
        private final SortedList<SolitaireRecord> timeList;
        private final SortedList<SolitaireRecord> stepList;

        LevelRecord(SolitaireRecord onlyRecord) {
            this(List.of(onlyRecord));
        }

        LevelRecord(List<SolitaireRecord> list) {
            timeList = new SortedList<>(Comparator.comparingInt(o -> o.seconds), list);
            stepList = new SortedList<>(Comparator.comparingInt(o -> o.steps), list);
            scoreList = new SortedList<>((o1, o2) -> -Integer.compare(o1.score, o2.score), list);
        }

        static LevelRecord fromJson(JSONArray jsonArray) {
            List<SolitaireRecord> list = new ArrayList<>();
            for (Object object : jsonArray) {
                if (object instanceof JSONObject) {
                    JSONObject jsonObject = (JSONObject) object;
                    try {
                        int score = jsonObject.getInt("score");
                        int seconds = jsonObject.getInt("seconds");
                        int steps = jsonObject.getInt("steps");
                        String dateStr = jsonObject.getString("date");
                        int initFin = jsonObject.getInt("initFinishes");
                        list.add(new SolitaireRecord(initFin, seconds, score, steps, DATE_FORMAT.parse(dateStr)));
                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
            return new LevelRecord(list);
        }

        JSONArray toJson() {
            JSONArray array = new JSONArray();
            for (SolitaireRecord recordItem : scoreList) {  // pick one
                JSONObject recordObj = new JSONObject();
                recordObj.put("score", recordItem.score);
                recordObj.put("seconds", recordItem.seconds);
                recordObj.put("steps", recordItem.steps);
                recordObj.put("initFinishes", recordItem.initFinish);
                recordObj.put("date", DATE_FORMAT.format(recordItem.date));
                array.put(recordObj);
            }
            return array;
        }

        /**
         * Returns [scoreRank, timeRank, stepsRank]
         */
        SolitaireRankResult insert(SolitaireRecord recordItem) {
            SolitaireRecord scoreBest = scoreList.getFirst();
            SolitaireRecord timeBest = timeList.getFirst();
            SolitaireRecord stepBest = stepList.getFirst();
            return new SolitaireRankResult(
                    scoreList.insert(recordItem),
                    scoreBest,
                    timeList.insert(recordItem),
                    timeBest,
                    stepList.insert(recordItem),
                    stepBest
            );
        }
    }
}

package it.polimi.nsds.spark;

import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;

/*
 * Group number: 38
 *
 * Group members
 *  - 10901682 - Fatih Temiz
 *  - 10900041 - Hessam Hashemizadeh
 *  - 10972566 - Mehmet Emre Akbulut
 */

public class SparkGroup38 {
    private static final int numCourses = 3000;

    public static void main(String[] args) throws TimeoutException {
        final String master = args.length > 0 ? args[0] : "local[4]";
        final String filePath = args.length > 1 ? args[1] : "./";

        final SparkSession spark = SparkSession
                .builder()
                .master(master)
                .appName("SparkEval")
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        final List<StructField> profsFields = new ArrayList<>();
        profsFields.add(DataTypes.createStructField("prof_name", DataTypes.StringType, false));
        profsFields.add(DataTypes.createStructField("course_name", DataTypes.StringType, false));
        final StructType profsSchema = DataTypes.createStructType(profsFields);

        final List<StructField> coursesFields = new ArrayList<>();
        coursesFields.add(DataTypes.createStructField("course_name", DataTypes.StringType, false));
        coursesFields.add(DataTypes.createStructField("course_hours", DataTypes.IntegerType, false));
        coursesFields.add(DataTypes.createStructField("course_students", DataTypes.IntegerType, false));
        final StructType coursesSchema = DataTypes.createStructType(coursesFields);

        final List<StructField> videosFields = new ArrayList<>();
        videosFields.add(DataTypes.createStructField("video_id", DataTypes.IntegerType, false));
        videosFields.add(DataTypes.createStructField("video_duration", DataTypes.IntegerType, false));
        videosFields.add(DataTypes.createStructField("course_name", DataTypes.StringType, false));
        final StructType videosSchema = DataTypes.createStructType(videosFields);

        // Professors: prof_name, course_name
        final Dataset<Row> profs = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(profsSchema)
                .csv(filePath + "files/profs.csv");

        // Courses: course_name, course_hours, course_students
        final Dataset<Row> courses = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(coursesSchema)
                .csv(filePath + "files/courses.csv");

        // Videos: video_id, video_duration, course_name
        final Dataset<Row> videos = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(videosSchema)
                .csv(filePath + "files/videos.csv");

        // Visualizations: value, timestamp
        // value represents the video id
        final Dataset<Row> visualizations = spark
                .readStream()
                .format("rate")
                .option("rowsPerSecond", 10)
                .load()
                .withColumn("value", col("value").mod(numCourses));

        /**
         * TODO: Enter your code below
         */

        // This dataset is used for q2 and q3.  It is cached because it is used multiple times to avoid unnecessary recomputation.
        final Dataset<Row> videosWithCourse = videos.join(courses, videos.col("course_name").equalTo(courses.col("course_name")))
                .select(videos.col("video_id"), videos.col("video_duration"), courses.col("course_name"), courses.col("course_students"));

        videosWithCourse.cache();

        // This dataset is used for q1. We did not use cache because it is used only once in q1.
        final Dataset<Row> profWithCourse = profs.join(courses, profs.col("course_name").equalTo(courses.col("course_name")))
                .select(profs.col("prof_name"), courses.col("course_name"), courses.col("course_hours"), courses.col("course_students"));


        /*
         * Query Q1. Compute the total number of lecture hours per prof
         */

        // We group by prof_name and sum the course_hours for each prof_name
        final Dataset<Row> q1 = profWithCourse.
                groupBy("prof_name").
                sum("course_hours").
                select("prof_name", "sum(course_hours)");

        q1.show();

        /*
         * Query Q2. For each course, compute the total duration of all the visualizations of videos of that course,
         * computed over a minute, updated every 10 seconds
         */

        // We join the visualizations dataset with the videosWithCourse dataset and we group by window and course_name.
        // We sum the video_duration for each course_name.
        final StreamingQuery q2 = visualizations.
                join(videosWithCourse, visualizations.col("value").equalTo(videosWithCourse.col("video_id"))).
                drop( "course_students").
                groupBy(window(col("timestamp"), "1 minute", "10 seconds"), col("course_name")).
                sum("video_duration").
                writeStream().
                outputMode("update").
                format("console").
                start();

        //We use output mode as  "update" since it is indicated that For streaming queries: write the results on the console, showing only the results that changed since the last evaluation

        /*
         * Query Q3. For each video, compute the total number of visualizations of that video
         * with respect to the number of students in the course in which the video is used.
         */
        // We join the visualizations dataset with the videosWithCourse dataset and we group by video_id and course_students.
        // We count the number of visualizations for each video_id. Grouping by course students is used to pass data to aggregation, does not affect the result.
        // We compute the ratio between the number of visualizations and the number of students in the course.
        final StreamingQuery q3 = visualizations.
                join(videosWithCourse, visualizations.col("value").equalTo(videosWithCourse.col("video_id"))).
                drop("video_duration", "course_name").
                groupBy("video_id", "course_students").
                agg(count("*").as("visualizations"), col("course_students")).
                withColumn("ratio", col("visualizations").divide(col("course_students"))).
                select("video_id", "ratio").
                writeStream().
                outputMode("update").
                format("console").
                start();


        try {
            q2.awaitTermination();
            q3.awaitTermination();
        } catch (final StreamingQueryException e) {
            e.printStackTrace();
        }

        spark.close();
    }
}
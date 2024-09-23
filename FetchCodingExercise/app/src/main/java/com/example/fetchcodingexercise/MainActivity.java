package com.example.fetchcodingexercise;

import android.app.ActivityManager;
import android.os.Bundle;
import android.util.JsonReader;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.SortedList;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class contains all of the code for the Fetch Coding Assessment.
 * This mobile app intends to parse a web-based JSON file with a list of items and display
 * any items with valid names.
 * @author Nikolaus Johnson
 */
public class MainActivity extends AppCompatActivity {
    /**
     * This class is used to define an Item object using an id, list id, and name collected from the
     * json file. This object implements the Comparable interface to allow for sorting by list id
     * and name.
     * @author Nikolaus Johnson
     */
    public static class Item implements Comparable<Item> {
        /** ID of the item. */
        public int id;
        /** Item's group. */
        public int listId;
        /** Name of the item. */
        public String name;

        /**
         * Constructor for the Item object. Initializes all fields of the object.
         * @param id ID of the item.
         * @param listId Group of the item.
         * @param name Name of the item.
         */
        public Item(int id, int listId, String name) {
            this.id = id;
            this.listId = listId;
            this.name = name;
        }

        @Override
        public int compareTo(Item item) {
            // Starts by comparing group id.
            if (this.listId != item.listId) {
                return this.listId - item.listId;
            }
            // If group ids are the same, compare based on name.
            else {
                return (this.name.compareTo(item.name));
            }
        }
    }

    /**
     * This method uses a JsonReader object for converting information from a JSON file to a list of
     * objects.
     * @param jr JsonReader object for gathering input from JSON.
     * @param itemList List of items to output objects from JSON to.
     * @throws IOException if there is an error reading information from the JSON.
     */
    public void readObject(JsonReader jr, List<Item> itemList) throws IOException {
        int id = -1;
        int listId = -1;
        String name = null;
        // Begins parsing a single object.
        jr.beginObject();
        // This while loop continues as long as there are fields left in the JSON object.
        while (jr.hasNext()) {
            // Collects a field identifier.
            String fieldName = jr.nextName();
            // Switch statement used to determine the field being entered.
            switch (fieldName) {
                // Reads an integer into the id field.
                case "id":
                    id = jr.nextInt();
                    break;
                // Reads an integer into the listId field.
                case "listId":
                    listId = jr.nextInt();
                    break;
                // Reads a string into the name field, or skips if the field is null.
                case "name":
                    try {
                        name = jr.nextString();
                    } catch (Exception e) {
                        jr.nextNull();
                    }
                    break;
            }
        }
        // If all fields are valid, creates a new item and adds to the item list.
        if (id != -1 && listId != -1 && name != null && !name.isEmpty()) {
            itemList.add(new Item(id, listId, name));
        }
        // Finishes parsing the object.
        jr.endObject();
    }

    /**
     * This class uses the Callable interface to run the parsing algorithm for the URL given.
     * @author Nikolaus Johnson
     */
    public class CallableParseTask implements Callable<List<Item>> {

        @Override
        public List<Item> call() throws Exception {
            // Declares an initializes a URL object, which links to the website containing the JSON
            // file.
            URL url;
            try {
                url = new URL("https://fetch-hiring.s3.amazonaws.com/hiring.json");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            // Initializes an item list to contain the items from the JSON.
            List<Item> itemList = new ArrayList<Item>();
            // Opens an input stream to collect information directly from the website.
            try (InputStream input = url.openStream()) {
                // Initializes a JsonReader object, which helps when parsing JSON input.
                JsonReader jr = new JsonReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                // Begins parsing from the first bracket.
                jr.beginArray();
                // Calls the readObject method for every item in the file.
                while (jr.hasNext()) {
                    readObject(jr, itemList);
                }
                // Finishes parsing with the last bracket.
                jr.endArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // Returns the item list.
            return itemList;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initial setup of the mobile app.
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Try-catch used in case any runtime errors occur.
        try {
            List<Item> itemList;
            List<Future<List<Item>>> futures;
            // Sets up cached thread pool to assist with asynchronous tasks.
            ExecutorService cached = Executors.newCachedThreadPool();
            // Calls the CallableParseTask class' call method to asynchronously collect input from
            // the JSON file and converting it into a list of items.
            futures = cached.invokeAll(Arrays.asList(new CallableParseTask()));
            // Collects the end result from the asynchronous task.
            itemList = futures.get(0).get();
            // Uses the Comparable interface to sort the item list.
            Collections.sort(itemList);
            // Creates a 2D array to act as a table.
            String[][] table = new String[itemList.size() + 1][];
            // Initializes the first row with the names of the items' fields.
            table[0] = new String[] {"id", "listId", "name"};
            // Iterates over the item list, adding every item's fields to the table.
            for (int i = 0; i < itemList.size(); i++) {
                Item item = itemList.get(i);
                table[i + 1] = new String[] {String.valueOf(item.id), String.valueOf(item.listId), item.name};
            }
            TextView text = (TextView) findViewById(R.id.textView);
            StringBuilder sb = new StringBuilder();
            // Iterates over the table's rows, adding them to a StringBuilder object formatted.
            for (final Object[] row : table) {
                sb.append(String.format("%25s%25s%25s%n", row));
            }
            // Converts the text in the StringBuilder to a String and outputs to the app.
            text.setText(sb.toString());
        }
        catch (Exception e) {
            // If any error occurs, throw a RuntimeException with the error message.
            throw new RuntimeException(e);
        }
    }
}
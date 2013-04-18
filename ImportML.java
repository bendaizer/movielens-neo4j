import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;

import org.neo4j.kernel.impl.util.FileUtils;
import org.neo4j.helpers.collection.MapUtil;

import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.neo4j.unsafe.batchinsert.BatchInserterImpl;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;

public class ImportML{
    private static final String DATA_DIR = "ml-100k/";
    private static final String OCCUPATION_FILE = DATA_DIR+"u.occupation";
    private static final String GENRE_FILE = DATA_DIR+"u.genre";
    private static final String USER_FILE = DATA_DIR+"u.user";
    private static final String ITEM_FILE = DATA_DIR+"u.item";
    private static final String DATA_FILE = DATA_DIR+"u.data";
    private static final String DB_PATH = "data/movielens.db";

    private static File db_directory = new File(DB_PATH);
    private static GraphDatabaseService graphDb;
    private static Relationship relationship;

    private static final Logger logger = Logger.getLogger("myLogger");

    public static void main(String[] args) {
        deleteFileOrDirectory(db_directory);
        try{

            List<HashMap<String,Object>> genres_list = new ArrayList<HashMap<String,Object>>();
            List<HashMap<String,Object>> occupations_list = new ArrayList<HashMap<String,Object>>();
            List<HashMap<String,Object>> users_list = new ArrayList<HashMap<String,Object>>();
            List<HashMap<String,Object>> movies_list = new ArrayList<HashMap<String,Object>>();
            List<HashMap<String,Object>> data_list = new ArrayList<HashMap<String,Object>>();


            genres_list = importFile(GENRE_FILE,new ArrayList<String>(Arrays.asList("genre","id_genre")));
            occupations_list = importFile(OCCUPATION_FILE,new ArrayList<String>(Arrays.asList("occupation")));


            ArrayList<String> user_label = new ArrayList<String>(Arrays.asList("user_id", "age", "gender",
                                                                            "occupation", "zip"));
            users_list = importFile(USER_FILE,user_label);


            ArrayList<String> movie_label = new ArrayList<String>(
                                            Arrays.asList("movie_id","title","date","imdb",
                                            "unknown","Action","Adventure","Animation","Children's","Comedy",
                                            "Crime","Documentary","Drama","Fantasy","Film-Noir","Horror",
                                            "Musical","Mystery","Romance","Sci-Fi","Thriller","War","Western")
                                            );
            movies_list = importFile(ITEM_FILE, movie_label);
            //FIXME there's a problem with "unknown“, it takes imdb field value, and imdb field is empty !

            data_list = importData(DATA_FILE,new ArrayList<String>(Arrays.asList("user_id","movie_id","rating","timestamp")));


            /*******/

            Map<String,Long> user_id_node = new HashMap<String, Long>();
            Map<String,Long> movie_id_node = new HashMap<String, Long>();
            Map<String,Long> genre_node = new HashMap<String, Long>();
            Map<String,Long> occupation_node = new HashMap<String, Long>();

            occupation_node = node_inserter(occupations_list, "occupations", "occupation");
            genre_node = node_inserter(genres_list, "genres", "genre");
            user_id_node = node_inserter(users_list, "users", "user_id");
            movie_id_node = node_inserter(movies_list, "movies", "movie_id");

            ArrayList<String> movie_genre = new ArrayList<String>(
                                            Arrays.asList("Action","Adventure","Animation","Children's","Comedy",
                                            "Crime","Documentary","Drama","Fantasy","Film-Noir","Horror",
                                            "Musical","Mystery","Romance","Sci-Fi","Thriller","War","Western")
                                            );

            for (String genre_label : movie_genre) {
                MovieGenreRelationshipInserter("IS_GENRE", movies_list, movie_id_node, genre_node, "movie_id", genre_label);
            }



            UserMovieRelationshipInserter(data_list, user_id_node, movie_id_node);


            // System.out.println(movie_genre.toString());

            // for ( Map m : data_list){
            //     System.out.println(m);
            // }


        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }finally{
            // indexProvider.shutdown();
            // inserter.shutdown();
        }
    }

    private static void UserMovieRelationshipInserter( List<HashMap<String,Object>> relation_map, Map<String,Long> node1_id, Map<String,Long> node2_id ){
        /****************************

        ****************************/

        logger.info("Inserting user rating movie relationship ");
        BatchInserter inserter = BatchInserters.inserter( DB_PATH );
        long node1, node2;
        String rating;

        for (Map<String, Object> map : relation_map) {
            try{
                node1=node1_id.get(map.get("user_id"));
                node2=node2_id.get(map.get("movie_id"));
                rating=map.get("rating").toString();

                RelationshipType reltype = DynamicRelationshipType.withName( rating );
        // To set properties on the relationship, use a properties map
        // instead of null as the last parameter.
                inserter.createRelationship( node1, node2, reltype, null );

            } catch(NullPointerException e){
                logger.warning("NullPointerException : relation will be ignored ...");
                // logger.warning("\t"+map+"    : "+node1_id.get(map.get(label1))+", "+node2_id.get(map.get(label2)));
            }
        }
        inserter.shutdown();
    }
    private static void RelationshipInserter(String relation_type, List<HashMap<String,Object>> relation_map, Map<String,Long> node1_id, Map<String,Long> node2_id, String label1, String label2 ){
        /****************************

        ****************************/

        logger.info("Inserting relationship "+relation_type);
        BatchInserter inserter = BatchInserters.inserter( DB_PATH );
        long node1, node2;

        for (Map<String, Object> map : relation_map) {
            try{
                node1=node1_id.get(map.get(label1));
                node2=node2_id.get(map.get(label2));
                RelationshipType reltype = DynamicRelationshipType.withName( relation_type );
                // logger.info(map.toString());
                // logger.info(map.get(label1).toString());
                // logger.info(node1_id.toString());

        // To set properties on the relationship, use a properties map
        // instead of null as the last parameter.

                inserter.createRelationship( node1, node2, reltype, null );

            } catch(NullPointerException e){
                logger.warning("NullPointerException : relation will be ignored ...");
                logger.warning("\t"+map+"    : "+node1_id.get(map.get(label1))+", "+node2_id.get(map.get(label2)));
            }
        }
        inserter.shutdown();
    }

    private static void MovieGenreRelationshipInserter(String relation_type, List<HashMap<String,Object>> relation_map, Map<String,Long> node1_id, Map<String,Long> node2_id, String label1, String label2 ){

        /****************************

        ****************************/

        // logger.info("Inserting relationship "+relation_type+" for genre : "+label2);
        BatchInserter inserter = BatchInserters.inserter( DB_PATH );
        long node1, node2;

        // logger.info("label1 : "+label1+" --- label2 : "+label2);

        // logger.info(label2+" : "+ node2_id.get(label2).toString());

                for (Map<String, Object> map : relation_map) {
                    if(map.get(label2).toString().equals("1")){
                    try{


                            // logger.info(map.get("title").toString()+" : "+ map.get(label2).toString());

                            node1=node1_id.get(map.get(label1));
                            node2=node2_id.get(label2);
                            RelationshipType reltype = DynamicRelationshipType.withName( relation_type );


                        // To set properties on the relationship, use a properties map
                        // instead of null as the last parameter.

                            inserter.createRelationship( node1, node2, reltype, null );


                    } catch(NullPointerException e){
                        logger.warning("NullPointerException : relation will be ignored ...");
                        logger.warning("\t"+map+"    : "+node1_id.get(map.get(label1))+", "+node2_id.get(label2));
                    }
                }}
        inserter.shutdown();
    }







    private static Map<String,Long> node_inserter(List<HashMap<String,Object>> nodes_map, String indexName, String indexProperty){
        /****************************
         Take list of properties, insert the corresponding nodes
         returns a map between ID or type and nodeID for relationships
         ****************************/
        // logger.info("Inserting nodes "+indexName);

        Map<String,Long> property_id = new HashMap<String, Long>();
        BatchInserter inserter = BatchInserters.inserter( DB_PATH );
        BatchInserterIndexProvider indexProvider = new LuceneBatchInserterIndexProvider( inserter );

        BatchInserterIndex theIndex = indexProvider.nodeIndex(indexName, MapUtil.stringMap("type", "exact"));
        theIndex.setCacheCapacity(indexProperty,500000);

        int size=0;

        for (Map<String, Object> map : nodes_map) {
            long node = inserter.createNode(map);
            theIndex.add(node,map);
            property_id.put(map.get(indexProperty).toString(),node);
            size++;
        }

        indexProvider.shutdown();
        inserter.shutdown();
        // logger.info("size of import : "+size);

        return property_id;

    }


    private static List<HashMap<String,Object>> importFile(String file, ArrayList<String> labels){
        FileInputStream fstream = null;
        BufferedReader reader = null;
        HashMap<String, Object> properties = new HashMap<String, Object>();
        List<HashMap<String,Object>> nodes_list = new ArrayList<HashMap<String,Object>>();

        try{
            fstream = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));
            String line;
            String resource_name;
            String[] tokens;
            //FIXME should compare labels_size with number of columns in file at come point!
            int num_column = labels.size();

            while ((line = reader.readLine()) != null){
                try{
                    tokens = line.split("\\|");
                    properties = new HashMap();
                    for (int i=0; i<num_column ; i++){
                        properties.put(labels.get(i),tokens[i]);
                    }
                    nodes_list.add(properties);

                }catch (Exception e){
                    logger.warning("Import of file "+file+" - Bad Line  :"+line);
                    logger.warning(e.getMessage());
                }

            }


        } catch (Exception e) {
            logger.warning("couldn't import file "+file);
            logger.warning(e.getMessage());
        }finally{
            try{
                fstream.close();
                reader.close();
            }catch(Exception e){
            }
        }

        return nodes_list;
    }

    private static List<HashMap<String,Object>> importData(String file, ArrayList<String> labels){
        /*
        Separate import method for u.data for 2 reasons:
        + u.data is tab separated whilst others are "|" separated
        + method will convert ratings to like/dislike at the same time
        */
        FileInputStream fstream = null;
        BufferedReader reader = null;
        HashMap<String, Object> properties = new HashMap<String, Object>();
        List<HashMap<String,Object>> nodes_list = new ArrayList<HashMap<String,Object>>();

        try{
            fstream = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));
            String line;
            String resource_name;
            String[] tokens;
            //FIXME should compare labels_size with number of columns in file at come point!
            int num_column = labels.size();


            while ((line = reader.readLine()) != null){
                try{
                    tokens = line.split("\\t");
                    properties = new HashMap();
                    for (int i=0; i<num_column ; i++){
                        if (labels.get(i).equals("rating")){
                            tokens[i] = Integer.parseInt(tokens[i])>2 ? "LIKE" : "DISLIKE";
                        }
                        properties.put(labels.get(i),tokens[i]);
                    }
                    nodes_list.add(properties);

                }catch (Exception e){
                    logger.warning("Import of file "+file+" - Bad Line  :"+line);
                    logger.warning(e.getMessage());
                }

            }


        } catch (Exception e) {
            logger.warning("couldn't import file "+file);
            logger.warning(e.getMessage());
        }finally{
            try{
                fstream.close();
                reader.close();
            }catch(Exception e){
            }
        }

        return nodes_list;
    }

    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }

    public static void deleteFileOrDirectory( final File file ) {
        // remove a directory with it's files and subdirectories
        if ( file.exists() ) {
            if ( file.isDirectory() ) {
                for ( File child : file.listFiles() ) {
                    deleteFileOrDirectory( child );
                }
            }
            file.delete();
        }
    }

}
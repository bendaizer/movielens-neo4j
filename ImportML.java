import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

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
    private static final String OCCUPATION_FILE= "u.occupation";
    private static final String GENRE_FILE= "u.genre";
    private static final String USER_FILE= "u.user";
    private static final String ITEM_FILE= "u.item";
    private static final String DATA_FILE= "u.data";

    private static final String DB_PATH = "data/movielens.db";
    private static File db_directory = new File(DB_PATH);
    private static GraphDatabaseService graphDb;
    private static Relationship relationship;

    private static final Logger logger = Logger.getLogger("myLogger");

    public static void main(String[] args) {
        deleteFileOrDirectory(db_directory);

        BatchInserter inserter = BatchInserters.inserter( DB_PATH );
        BatchInserterIndexProvider indexProvider = new LuceneBatchInserterIndexProvider( inserter );
        BatchInserterIndex user_index = indexProvider.nodeIndex("users", MapUtil.stringMap("type", "exact"));
        BatchInserterIndex movie_index = indexProvider.nodeIndex("movies", MapUtil.stringMap("type", "exact"));
        BatchInserterIndex genre_index = indexProvider.nodeIndex("genres", MapUtil.stringMap("type", "exact"));
        BatchInserterIndex occupation_index = indexProvider.nodeIndex("occupation", MapUtil.stringMap("type", "exact"));
        try{

            logger.info("it works, let's get started");

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }finally{
            indexProvider.shutdown();
            inserter.shutdown();
        }


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
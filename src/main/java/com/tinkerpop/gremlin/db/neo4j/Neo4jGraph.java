package com.tinkerpop.gremlin.db.neo4j;

import com.tinkerpop.gremlin.db.neo4j.util.Neo4jGraphEdgeIterable;
import com.tinkerpop.gremlin.db.neo4j.util.Neo4jVertexIterable;
import com.tinkerpop.gremlin.model.Edge;
import com.tinkerpop.gremlin.model.Graph;
import com.tinkerpop.gremlin.model.Index;
import com.tinkerpop.gremlin.model.Vertex;
import com.tinkerpop.gremlin.statements.EvaluationException;
import org.neo4j.graphdb.*;
import org.neo4j.index.Isolation;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.kernel.impl.transaction.TransactionFailureException;

import java.io.File;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class Neo4jGraph implements Graph {

    private GraphDatabaseService neo;
    private String directory;
    private Neo4jIndex index;
    private Transaction tx;
    private boolean automaticTransactions = true;

    public Neo4jGraph(final String directory) {
        this.directory = directory;
        this.neo = new EmbeddedGraphDatabase(this.directory);
        LuceneIndexService indexService = new LuceneIndexService(neo);
        indexService.setIsolation(Isolation.SAME_TX);
        this.index = new Neo4jIndex(indexService, this);
        if (this.automaticTransactions) {
            this.tx = neo.beginTx();
        }
    }
    public Neo4jGraph(final GraphDatabaseService neo) {
        this.neo=neo;
        this.directory=((EmbeddedGraphDatabase)neo).getStoreDir();
        this.automaticTransactions=false;
        LuceneIndexService indexService = new LuceneIndexService(neo);
        indexService.setIsolation(Isolation.SAME_TX);
        this.index = new Neo4jIndex(indexService, this);
    }

    public Index getIndex() {
        return this.index;
    }

    public Vertex addVertex(final Object id) {
        Vertex vertex = new Neo4jVertex(neo.createNode(), this.index, this);
        this.stopStartTransaction();
        return vertex;
    }

    public Vertex getVertex(final Object id) {
        if (null == id)
            return null;

        try {
            Long longId = Double.valueOf(id.toString()).longValue();
            Node node = this.neo.getNodeById(longId);
            return new Neo4jVertex(node, this.index, this);
        } catch (NotFoundException e) {
            return null;
        } catch (NumberFormatException e) {
            throw new EvaluationException("Neo vertex ids must be convertible to a long value.");
        }
    }

    public Iterable<Vertex> getVertices() {
        return new Neo4jVertexIterable(this.neo.getAllNodes(), this);
    }

    public Iterable<Edge> getEdges() {
        return new Neo4jGraphEdgeIterable(this.neo.getAllNodes(), this);
    }

    public void removeVertex(final Vertex vertex) {

        Long id = (Long) vertex.getId();
        Node node = neo.getNodeById(id);
        if (null != node) {
            for (String key : vertex.getPropertyKeys()) {
                this.index.remove(key, vertex.getProperty(key), vertex);
            }
            for (Edge edge : vertex.getInEdges()) {
                this.removeEdge(edge);
            }
            for (Edge edge : vertex.getOutEdges()) {
                this.removeEdge(edge);
            }
            node.delete();
            this.stopStartTransaction();
        }
    }

    public Edge addEdge(final Object id, final Vertex outVertex, final Vertex inVertex, final String label) {
        Node outNode = (Node) ((Neo4jVertex) outVertex).getRawElement();
        Node inNode = (Node) ((Neo4jVertex) inVertex).getRawElement();
        Relationship relationship = outNode.createRelationshipTo(inNode, DynamicRelationshipType.withName(label));
        this.stopStartTransaction();
        return new Neo4jEdge(relationship, this.index, this);
    }

    public void removeEdge(Edge edge) {
        ((Relationship) ((Neo4jEdge) edge).getRawElement()).delete();
        this.stopStartTransaction();
    }

    protected void stopStartTransaction() {
        if (this.automaticTransactions) {
            if (null != tx) {
                this.tx.success();
                this.tx.finish();
                this.tx = neo.beginTx();
            } else {
                throw new EvaluationException("There is no active transaction to stop");
            }
        }
    }

    public void startTransaction() {
        if (this.automaticTransactions)
            throw new EvaluationException("Turn off automatic transactions to use manual transaction handling");

        this.tx = neo.beginTx();
    }

    public void stopTransaction(boolean success) {
        if (this.automaticTransactions)
            throw new EvaluationException("Turn off automatic transactions to use manual transaction handling");

        if (success) {
            this.tx.success();
        } else {
            this.tx.failure();
        }
        this.tx.finish();
    }

    public void setAutoTransactions(boolean automatic) {
        this.automaticTransactions = automatic;
        if (null != this.tx) {
            this.tx.success();
            this.tx.finish();
        }
    }

    public void shutdown() {
        if (this.automaticTransactions) {
            try {
                this.tx.success();
                this.tx.finish();
            } catch (TransactionFailureException e) {
            }
        }
        this.neo.shutdown();
        this.index.shutdown();

    }

    public void clear() {
        this.shutdown();
        deleteGraphDirectory(new File(this.directory));
        this.neo = new EmbeddedGraphDatabase(this.directory);
        LuceneIndexService indexService = new LuceneIndexService(neo);
        indexService.setIsolation(Isolation.SAME_TX);
        this.index = new Neo4jIndex(indexService, this);
        this.tx = neo.beginTx();
        this.removeVertex(this.getVertex(0));
        this.stopStartTransaction();
    }

    private static void deleteGraphDirectory(final File directory) {
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    deleteGraphDirectory(file);
                }
                file.delete();
            }
        }
    }


    public String toString() {
        EmbeddedGraphDatabase embeddedNeo = (EmbeddedGraphDatabase) neo;
        NodeManager nodeManager = embeddedNeo.getConfig().getNeoModule().getNodeManager();
        return "neograph[db:" + this.directory + ", vertices:" + nodeManager.getNumberOfIdsInUse(Node.class) + ", edges:" + nodeManager.getNumberOfIdsInUse(Relationship.class) + "]";
    }
}

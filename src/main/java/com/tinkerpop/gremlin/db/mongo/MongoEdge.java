package com.tinkerpop.gremlin.db.mongo;

import com.mongodb.DBObject;
import com.tinkerpop.gremlin.db.StringFactory;
import com.tinkerpop.gremlin.model.Edge;
import com.tinkerpop.gremlin.model.Vertex;
import com.tinkerpop.gremlin.statements.Tokens;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class MongoEdge extends MongoElement implements Edge {


    public MongoEdge(final DBObject dbObject, final MongoGraph graph) {
        super(dbObject, graph);
    }

    public String getLabel() {
        return this.dbObject.get(Tokens.LABEL).toString();
    }

    public Vertex getOutVertex() {
        Object id = this.dbObject.get(Tokens.OUT_VERTEX);
        return graph.getVertex(id);

    }

    public Vertex getInVertex() {
        Object id = this.dbObject.get(Tokens.IN_VERTEX);
        return graph.getVertex(id);
    }

    public boolean equals(final Object object) {
        return object instanceof MongoEdge && ((MongoEdge) object).getId().equals(this.getId());
    }

    public String toString() {
        return StringFactory.edgeString(this);
    }
}

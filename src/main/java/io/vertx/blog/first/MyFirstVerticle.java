package io.vertx.blog.first;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a verticle. A verticle is a _Vert.x component_. This verticle is implemented in Java, but you can
 * implement them in JavaScript, Groovy, Ruby or Ceylon.
 */
public class MyFirstVerticle extends AbstractVerticle {
  public static final String COLLECTION_POEM = "poems";
  private MongoClient mongo;

  /**
   * This method is called when the verticle is deployed. It creates a HTTP server and registers a simple request
   * handler.
   * <p/>
   * Notice the `listen` method. It passes a lambda checking the port binding result. When the HTTP server has been
   * bound on the port, it call the `complete` method to inform that the starting has completed. Else it reports the
   * error.
   *
   * @param fut the future
   */
  @Override
  public void start(Future<Void> fut) {
    mongo = MongoClient.createShared(vertx, config());
    createSomeData(
        (nothing) -> startWebApp(
            (http) -> completeStartup(http, fut)
        ), fut);
  }

  private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
    // Create a router object.
    Router router = Router.router(vertx);

    // Bind "/" to our hello message.
    router.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response
          .putHeader("content-type", "text/html")
          .end("<h1>Hello from my first Vert.x 3 application</h1>");
    });

    router.route("/assets/*").handler(StaticHandler.create("assets"));

    router.get("/api/poems").handler(this::getAll);
    router.route("/api/poems*").handler(BodyHandler.create());
    router.post("/api/poems").handler(this::addOne);
    router.get("/api/poems/:id").handler(this::getOne);
    router.put("/api/poems/:id").handler(this::updateOne);
    router.delete("/api/poems/:id").handler(this::deleteOne);


    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
        .createHttpServer()
        .requestHandler(router::accept)
        .listen(
            // Retrieve the port from the configuration,
            // default to 8080.
            config().getInteger("http.port", 8080),
            next::handle
        );
  }

  private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
    if (http.succeeded()) {
      fut.complete();
    } else {
      fut.fail(http.cause());
    }
  }


  @Override
  public void stop() throws Exception {
    mongo.close();
  }

  private void addOne(RoutingContext routingContext) {
	System.out.println(routingContext.getBodyAsString());
    final Poem whisky = Json.decodeValue(routingContext.getBodyAsString(),
    		Poem.class);

    mongo.insert(COLLECTION_POEM, whisky.toJson(), r ->
        routingContext.response()
            .setStatusCode(201)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(whisky.setId(r.result()))));
  }

  private void getOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      mongo.findOne(COLLECTION_POEM, new JsonObject().put("_id", id), null, ar -> {
        if (ar.succeeded()) {
          if (ar.result() == null) {
            routingContext.response().setStatusCode(404).end();
            return;
          }
          Poem whisky = new Poem(ar.result());
          routingContext.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(Json.encodePrettily(whisky));
        } else {
          routingContext.response().setStatusCode(404).end();
        }
      });
    }
  }

  private void updateOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    JsonObject json = routingContext.getBodyAsJson();
    if (id == null || json == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      mongo.update(COLLECTION_POEM,
          new JsonObject().put("_id", id), // Select a unique document
          // The update syntax: {$set, the json object containing the fields to update}
          new JsonObject()
              .put("$set", json),
          v -> {
            if (v.failed()) {
              routingContext.response().setStatusCode(404).end();
            } else {
              routingContext.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(Json.encodePrettily(new Poem(id, json.getString("header"), json.getString("content"), json.getString("useDay"), json.getString("imgUrl"), json.getString("attach"))));
            }
          });
    }
  }

  private void deleteOne(RoutingContext routingContext) {
    String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      mongo.removeOne(COLLECTION_POEM, new JsonObject().put("_id", id),
          ar -> routingContext.response().setStatusCode(204).end());
    }
  }

  private void getAll(RoutingContext routingContext) {
    mongo.find(COLLECTION_POEM, new JsonObject(), results -> {
      List<JsonObject> objects = results.result();
      List<Poem> poems = objects.stream().map(Poem::new).collect(Collectors.toList());
      routingContext.response()
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(poems));
    });
  }

  private void createSomeData(Handler<AsyncResult<Void>> next, Future<Void> fut) {
	  Poem bowmore = new Poem("A", "B","C","D","E");
	  Poem talisker = new Poem("1", "2","3","4","5");
    // Do we have data in the collection ?
    mongo.count(COLLECTION_POEM, new JsonObject(), count -> {
      if (count.succeeded()) {
        if (count.result() == 0) {
          mongo.insert(COLLECTION_POEM, bowmore.toJson(), ar -> {
            if (ar.failed()) {
              fut.fail(ar.cause());
            } else {
              mongo.insert(COLLECTION_POEM, talisker.toJson(), ar2 -> {
                if (ar2.failed()) {
                  fut.failed();
                } else {
                  next.handle(Future.<Void>succeededFuture());
                }
              });
            }
          });
        } else {
          next.handle(Future.<Void>succeededFuture());
        }
      } else {
        // report the error
        fut.fail(count.cause());
      }
    });
  }
}

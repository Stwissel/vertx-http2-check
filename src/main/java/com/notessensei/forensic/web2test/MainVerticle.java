package com.notessensei.forensic.web2test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;

public class MainVerticle extends AbstractVerticle {

  private JWTAuth authProvider;

  static final boolean SWITCH_ON_TLS = true;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    HttpServerOptions serverOptions = new HttpServerOptions()
        .setPort(8888)
        .setHost("frascati.projectkeep.local")
        .setSsl(SWITCH_ON_TLS)
        .setUseAlpn(true)
        .setOpenSslEngineOptions(new OpenSSLEngineOptions())
        .setPfxKeyCertOptions(
            new PfxOptions()
                .setPath("src/main/resources/frascati.projectkeep.local.pfx")
                .setPassword(System.getenv("pfxpassword")));

    Router mainRouter = Router.router(getVertx());

    this.setupDefaultRoutes(mainRouter)
        .compose(v -> getJwtAuthprovider())
        .compose(this::getOpenApiRouter)
        .compose(subRouter -> {
          mainRouter.route("/api/*").subRouter(subRouter);
          return getVertx().createHttpServer(serverOptions)
              .requestHandler(mainRouter)
              .listen();
        })
        .onSuccess(http -> {
          startPromise.complete();
          System.out.println("HTTPServer up and running");
        })
        .onFailure(startPromise::fail);
  }

  Future<Void> setupDefaultRoutes(final Router router) {

    // Default routing stuff here
    final StaticHandler rootHandler = StaticHandler.create();

    // Content security headers for URL endpoints
    final Handler<RoutingContext> strictCSP = ctx -> {
      ctx.response().putHeader("Content-Security-Policy", "default-src 'self';");
      ctx.next();
    };

    // We love Terry
    final Handler<RoutingContext> handlerPratchett = ctx -> {
      ctx.response().putHeader("X-Clacks-Overhead", "GNU Terry Pratchett");
      ctx.next();
    };

    final BodyHandler bodyHandler = BodyHandler.create();

    // General handlers needed for all requests
    router.route().handler(FaviconHandler.create(this.getVertx(), "webroot/favicon.ico"));
    router.route().handler(handlerPratchett);
    router.route().handler(bodyHandler);
    router.route("/").handler(strictCSP);
    router.route("/").handler(rootHandler);
    router.route("/assets/*").handler(strictCSP);
    router.route("/assets/*").handler(rootHandler);
    return Future.succeededFuture();
  }

  Future<Router> getOpenApiRouter(final JWTAuth jwtAuthprovider) {

    Promise<Router> promise = Promise.promise();

    Handler<RoutingContext> failureHandler = ctx -> ctx.response()
        .setStatusCode(500)
        .setStatusMessage(ctx.failure().getMessage())
        .end(ctx.failure().getMessage());

    RouterBuilder.create(getVertx(), "src/main/resources/yatdda.json")
        .onSuccess(rb -> {
          rb.operation("authLogin")
              .handler(this.getLoginHandler())
              .failureHandler(failureHandler);

          rb.operation("gettasks")
              .handler(this.getArrayHandler())
              .failureHandler(failureHandler);

          rb.securityHandler("jwt", JWTAuthHandler.create(jwtAuthprovider));
          promise.complete(rb.createRouter());
        })
        .onFailure(promise::fail);

    return promise.future();
  }

  Handler<RoutingContext> getLoginHandler() {

    return ctx -> {
      JsonObject j = ctx.body().asJsonObject();
      if (!"password".equalsIgnoreCase(j.getString("password"))) {
        ctx.fail(new Exception("I wanted a password"));
      } else {
        final String token = this.authProvider.generateToken(j);
        ctx.response().putHeader("Content-Type", "application/json")
            .end(new JsonObject()
                .put("username", j.getString("username"))
                .put("token", token)
                .toBuffer());
      }
    };
  }

  Handler<RoutingContext> getArrayHandler() {
    return ctx -> ctx.end(
        new JsonArray()
            .add(new JsonObject().put("id", "task1"))
            .add(new JsonObject().put("id", "task2"))
            .add(new JsonObject().put("id", "task3"))
            .toBuffer());
  }

  Future<JWTAuth> getJwtAuthprovider() {
    this.authProvider = JWTAuth.create(vertx, new JWTAuthOptions()
        .addPubSecKey(new PubSecKeyOptions()
            .setAlgorithm("HS256")
            .setBuffer("supersecret for us")));

    return Future.succeededFuture(this.authProvider);
  }

}

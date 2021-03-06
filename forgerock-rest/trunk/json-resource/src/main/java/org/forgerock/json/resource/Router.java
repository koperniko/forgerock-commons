/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.http.Context;
import org.forgerock.http.context.ServerContext;
import org.forgerock.http.routing.AbstractRouter;
import org.forgerock.http.routing.IncomparableRouteMatchException;
import org.forgerock.http.routing.RouteMatcher;
import org.forgerock.http.routing.RouterContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;

import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.json.resource.Resources.newCollection;
import static org.forgerock.json.resource.Resources.newSingleton;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;
import static org.forgerock.util.promise.Promises.newExceptionPromise;

/**
 * A router which routes requests based on route predicates. Each route is
 * comprised of a {@link RouteMatcher route matcher} and a corresponding
 * handler, when routing a request the router will call
 * {@link RouteMatcher<Request>#evaluate(Context, Request)} for each
 * registered route and use the returned {@link RouteMatcher} to determine
 * which route best matches the request.
 *
 * <p>Routes may be added and removed from a router as follows:
 *
 * <pre>
 * Handler users = ...;
 * Router router = new Router();
 * RouteMatcher routeOne = RouteMatchers.requestUriMatcher(EQUALS, &quot;users&quot;);
 * RouteMatcher routeTwo = RouteMatchers.requestUriMatcher(EQUALS, &quot;users/{userId}&quot;);
 * router.addRoute(routeOne, users);
 * router.addRoute(routeTwo, users);
 *
 * // Deregister a route.
 * router.removeRoute(routeOne, routeTwo);
 * </pre></p>
 *
 * @see AbstractRouter
 * @see RouteMatchers
 *
 * @since 3.0.0
 */
public class Router extends AbstractRouter<Router, Request, RequestHandler> implements RequestHandler {

    /**
     * Creates a new router with no routes defined.
     */
    public Router() {
        super();
    }

    /**
     * Creates a new router containing the same routes and default route as the
     * provided router. Changes to the returned router's routing table will not
     * impact the provided router.
     *
     * @param router The router to be copied.
     */
    public Router(AbstractRouter<Router, Request, RequestHandler> router) {
        super(router);
    }

    @Override
    protected Router getThis() {
        return this;
    }

    /**
     * Adds a new route to this router for the provided collection resource
     * provider. New routes may be added while this router is processing
     * requests.
     * <p>
     * The provided URI template must match the resource collection itself, not
     * resource instances. For example:
     *
     * <pre>
     * CollectionResourceProvider users = ...;
     * UriRouter router = new UriRouter();
     *
     * // This is valid usage: the template matches the resource collection.
     * router.addRoute("users", users);
     *
     * // This is invalid usage: the template matches resource instances.
     * router.addRoute("users/{userId}", users);
     * </pre>
     *
     * @param uriTemplate
     *            The URI template which request resource names must match.
     * @param provider
     *            The collection resource provider to which matching requests
     *            will be routed.
     */
    public void addRoute(String uriTemplate, CollectionResourceProvider provider) {
        addRoute(requestUriMatcher(STARTS_WITH, uriTemplate), newCollection(provider));
    }

    /**
     * Adds a new route to this router for the provided singleton resource
     * provider. New routes may be added while this router is processing
     * requests.
     *
     * @param uriTemplate
     *            The URI template which request resource names must match.
     * @param provider
     *            The singleton resource provider to which matching requests
     *            will be routed.
     */
    public void addRoute(String uriTemplate, SingletonResourceProvider provider) {
        addRoute(requestUriMatcher(EQUALS, uriTemplate), newSingleton(provider));
    }

    private Pair<Context, RequestHandler> getBestMatch(ServerContext context, Request request)
            throws ResourceException {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestRoute(context, request);
            if (bestMatch == null) {
                throw new NotFoundException(String.format("Resource '%s' not found", request.getResourcePath()));
            } else {
                return bestMatch;
            }
        } catch (IncomparableRouteMatchException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    @Override
    public Promise<JsonValue, ResourceException> handleAction(ServerContext context, ActionRequest request) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            RouterContext routerContext = getRouterContext(bestMatch.getFirst());
            ActionRequest routedRequest = wasRouted(routerContext, request)
                    ? copyOfActionRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handleAction((ServerContext) bestMatch.getFirst(), routedRequest);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handleCreate(ServerContext context, CreateRequest request) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            RouterContext routerContext = getRouterContext(bestMatch.getFirst());
            CreateRequest routedRequest = wasRouted(routerContext, request)
                    ? copyOfCreateRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handleCreate((ServerContext) bestMatch.getFirst(), routedRequest);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handleDelete(ServerContext context, DeleteRequest request) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            RouterContext routerContext = getRouterContext(bestMatch.getFirst());
            DeleteRequest routedRequest = wasRouted(routerContext, request)
                    ? copyOfDeleteRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handleDelete((ServerContext) bestMatch.getFirst(), routedRequest);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handlePatch(ServerContext context, PatchRequest request) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            RouterContext routerContext = getRouterContext(bestMatch.getFirst());
            PatchRequest routedRequest = wasRouted(routerContext, request)
                    ? copyOfPatchRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handlePatch((ServerContext) bestMatch.getFirst(), routedRequest);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<QueryResult, ResourceException> handleQuery(ServerContext context, QueryRequest request, QueryResourceHandler handler) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            RouterContext routerContext = getRouterContext(bestMatch.getFirst());
            QueryRequest routedRequest = wasRouted(routerContext, request)
                    ? copyOfQueryRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handleQuery((ServerContext) bestMatch.getFirst(), routedRequest, handler);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handleRead(ServerContext context, ReadRequest request) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            RouterContext routerContext = getRouterContext(bestMatch.getFirst());
            ReadRequest routedRequest = wasRouted(routerContext, request)
                    ? copyOfReadRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handleRead((ServerContext) bestMatch.getFirst(), routedRequest);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handleUpdate(ServerContext context, UpdateRequest request) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            RouterContext routerContext = getRouterContext(bestMatch.getFirst());
            UpdateRequest routedRequest = wasRouted(routerContext, request)
                    ? copyOfUpdateRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handleUpdate((ServerContext) bestMatch.getFirst(), routedRequest);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    private RouterContext getRouterContext(Context context) {
        if (context.containsContext(RouterContext.class)) {
            return context.asContext(RouterContext.class);
        } else {
            return null;
        }
    }

    private boolean wasRouted(RouterContext routerContext, Request request) {
        return routerContext != null && !routerContext.getRemainingUri().equals(request.getResourcePath());
    }

    private String getResourcePath(RouterContext routerContext) {
        return routerContext.getRemainingUri();
    }
}

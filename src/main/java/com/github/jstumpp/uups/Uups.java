package com.github.jstumpp.uups;

import com.github.jstumpp.uups.views.UupsLayout;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * <h1>Uups!</h1>
 * <p> Pretty error page that helps you debug your web application. </p>
 * <p> <strong>NOTE</strong>: This module is base on <a href="https://github.com/filp/whoops">uups</a>
 * and uses the same front end resources.</p>
 */

public class Uups {

    private static final String HANDLER = "org.jooby.internal.HttpHandlerImpl";

    private static int SAMPLE_SIZE = 10;

    private static String openWith = "http://localhost:63342/api/file/?file=%s&line=%s";

    static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    static String readString(final ClassLoader loader, final String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(loader.getResource("uups/" + path).toURI())));
        } catch (Exception e) {
        }
        return "";
    }

    static List<UupsFrame> frames(final ClassLoader loader, final SourceLocator locator,
                                  final Throwable cause) {
        List<StackTraceElement> stacktrace = Arrays.asList(cause.getStackTrace());
        int limit = IntStream.range(0, stacktrace.size())
                .filter(i -> stacktrace.get(i).getClassName().equals(HANDLER)).findFirst()
                .orElse(stacktrace.size());
        return stacktrace.stream()
                // trunk stack at HttpHandlerImpl (skip servers stack)
                .limit(limit)
                .map(e -> frame(loader, locator, cause, e))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("rawtypes")
    static UupsFrame frame(final ClassLoader loader, final SourceLocator locator,
                           final Throwable cause, final StackTraceElement e) {
        int line = Math.max(e.getLineNumber(), 1);
        String className = e.getClassName();
        SourceLocator.Source source = locator.source(className);
        int[] range = source.range(line, SAMPLE_SIZE);
        int lineStart = range[0];
        int lineNth = line - lineStart;
        Path filePath = source.getPath();
        Optional<Class> clazz = findClass(loader, className);
        String filename = Optional.ofNullable(e.getFileName()).orElse("~unknown");
        UupsFrame frame = new UupsFrame();
        frame.fileName = new File(filename).getName();
        frame.methodName = Optional.ofNullable(e.getMethodName()).orElse("~unknown");
        frame.lineNumber = line;
        frame.lineStart = lineStart + 1;
        frame.lineNth = lineNth;
        frame.location = Optional.ofNullable(clazz.map(Uups::locationOf)
                .orElse(new File(filename).getParent())).orElse(filename);
        frame.source = source.source(range[0], range[1]);
        frame.open = String.format(openWith, filePath, line);
        frame.type = clazz.map(c -> c.getSimpleName()).orElse(new File(filename).getName());
        UupsFrameComment uupsFrameComment = new UupsFrameComment();
        uupsFrameComment.context = cause.getClass().getName();
        uupsFrameComment.text = cause.getMessage();
        frame.comments = new LinkedList<>();
        frame.comments.add(uupsFrameComment);
        return frame;
    }

    @SuppressWarnings("rawtypes")
    static String locationOf(final Class clazz) {
        return Optional.ofNullable(clazz.getResource(clazz.getSimpleName() + ".class"))
                .map(url -> {
                    try {
                        String path = url.getPath();
                        int i = path.indexOf("!");
                        if (i > 0) {
                            // jar url
                            String jar = path.substring(0, i);
                            return jar.substring(Math.max(jar.lastIndexOf('/'), -1) + 1);
                        }
                        String cfile = clazz.getName().replace(".", "/") + ".class";
                        String relativePath = path.replace(cfile, "");
                        return new File(System.getProperty("user.dir"))
                                .toPath()
                                .relativize(Paths.get(relativePath).toFile().getCanonicalFile().toPath())
                                .toString();
                    } catch (Exception e) {
                        return "~unknown";
                    }
                })
                .orElse("~unknown");
    }

    @SuppressWarnings("rawtypes")
    static Optional<Class> findClass(final ClassLoader loader, final String name) {
        return Arrays
                .asList(loader, Thread.currentThread().getContextClassLoader())
                .stream()
                // we don't care about exception
                .map(cl -> {
                    try {
                        return cl.loadClass(name);
                    } catch (ClassNotFoundException e) {
                        return (Class) null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    static List<Throwable> getCausalChain(Throwable throwable) {
        List<Throwable> causes = new ArrayList<>(4);
        causes.add(throwable);

        // Keep a second pointer that slowly walks the causal chain. If the fast pointer ever catches
        // the slower pointer, then there's a loop.
        Throwable slowPointer = throwable;
        boolean advanceSlowPointer = false;

        Throwable cause;
        while ((cause = throwable.getCause()) != null) {
            throwable = cause;
            causes.add(throwable);

            if (throwable == slowPointer) {
                throw new IllegalArgumentException("Loop in causal chain detected.", throwable);
            }
            if (advanceSlowPointer) {
                slowPointer = slowPointer.getCause();
            }
            advanceSlowPointer = !advanceSlowPointer; // only advance every other iteration
        }
        return Collections.unmodifiableList(causes);
    }

    /**
     * @param throwable
     * @param status
     * @param method
     * @param path
     * @param req
     * @return HTML to render Uups! page
     */
    @SuppressWarnings("unchecked")
    public static String renderHtml(Throwable throwable, int status, String method, String path, HttpServletRequest req) {
        // request params
        Map<String, String> requestParams = ((Stream<String>) Collections.list(((HttpServletRequest) req).getParameterNames())
                .stream())
                .collect(Collectors.toMap(p -> p, req::getParameter));

        // request locals
        Map<String, String> requestLocals = ((Stream<String>) Collections.list(((HttpServletRequest) req).getAttributeNames())
                .stream())
                .collect(Collectors.toMap((a) -> a, (a) -> req.getAttribute(a.toString()).toString()));


        // http headers
        Map<String, String> requestHeaders = ((Stream<String>) Collections.list(((HttpServletRequest) req).getHeaderNames())
                .stream())
                .map(Object::toString)
                .collect(Collectors.toMap(h -> h, req::getHeader));

        // session
        Map<String, String> session = ((Stream<String>) Collections.list(((HttpServletRequest) req).getSession().getAttributeNames())
                .stream())
                .map(Object::toString)
                .collect(Collectors.toMap(a -> a, a -> req.getSession().getAttribute(a.toString()).toString()));

        return renderHtml(throwable, status, method, path, requestParams, requestLocals, requestHeaders, session);
    }


    /**
     * @param throwable
     * @param status
     * @param method
     * @param path
     * @param requestParams
     * @param requestLocals
     * @param requestHeaders
     * @param session
     * @return HTML to render Uups! page
     */
    public static String renderHtml(Throwable throwable, int status, String method, String path, Map<String, String> requestParams, Map<String, String> requestLocals, Map<String, String> requestHeaders, Map<String, String> session) {
        ClassLoader loader = ClassLoader.getSystemClassLoader();

        SourceLocator locator = SourceLocator.local();
        int maxStackSize = 8;

        // only html, ignore any other request and fallback to default handler
        // if (req.getHeaders(MediaType.TEXT_HTML.getType()).hasMoreElements()) {

        //if (req.accepts(MediaType.html).isPresent()) {
        // is this a real Err? or we just wrap it?
        // dump causes as a list

        List<Throwable> causal = getCausalChain(throwable);

        // get the cause (initial exception)
        Throwable head = causal.get(causal.size() - 1);
        String message = head.getMessage();

        Map<String, Map<String, String>> envdata = new LinkedHashMap<>();

        // response
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", String.valueOf(status));
        envdata.put("response", response);

        // route
        Map<String, String> route = new LinkedHashMap<>();
        route.put("method", method);
        route.put("path", path);
        envdata.put("route", route);

        envdata.put("request params", requestParams);
        envdata.put("request locals", requestLocals);
        envdata.put("request headers", requestHeaders);
        envdata.put("session", session);

        List<UupsFrame> frames = causal.stream().filter(it -> it != head)
                .map(it -> frame(loader, locator, it, it.getStackTrace()[0]))
                .collect(Collectors.toList());

        frames.addAll(frames(loader, locator, head));

        // truncate frames
        frames = frames.subList(0, Math.min(maxStackSize, frames.size()));

        String css = readString(loader, "css/whoops.base.css");
        String clipboard = readString(loader, "js/clipboard.min.js");
        String prettify = readString(loader, "js/prettify.min.js");
        String js = readString(loader, "js/whoops.base.js");
        String zepto = readString(loader, "js/zepto.min.js");

        String query = "";
        try {
            query = URLEncoder.encode(head.getClass().getName() + ": " + message, "UTF-8");
        } catch (Exception ex) {
        }

        return UupsLayout.template("Error", message, zepto, clipboard, js, prettify, css, envdata, frames, causal, message, query, getStackTraceAsString(throwable)).render().toString();
    }
}

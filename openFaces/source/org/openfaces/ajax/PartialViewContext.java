/*
 * OpenFaces - JSF Component Library 3.0
 * Copyright (C) 2007-2010, TeamDev Ltd.
 * licensing@openfaces.org
 * Unless agreed in writing the contents of this file are subject to
 * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * Please visit http://openfaces.org/licensing/ for more details.
 */

package org.openfaces.ajax;

import org.openfaces.application.ViewExpiredExceptionHandler;
import org.openfaces.component.ComponentWithExternalParts;
import org.openfaces.component.OUIObjectIterator;
import org.openfaces.component.ajax.AjaxSettings;
import org.openfaces.component.ajax.DefaultSessionExpiration;
import org.openfaces.component.ajax.SilentSessionExpiration;
import org.openfaces.component.table.AbstractTable;
import org.openfaces.event.AjaxActionEvent;
import org.openfaces.org.json.JSONArray;
import org.openfaces.org.json.JSONException;
import org.openfaces.org.json.JSONObject;
import org.openfaces.org.json.JSONString;
import org.openfaces.renderkit.AjaxPortionRenderer;
import org.openfaces.util.*;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.MethodNotFoundException;
import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIForm;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialResponseWriter;
import javax.faces.context.PartialViewContextWrapper;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.Renderer;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dmitry Pikhulya
 */
public class PartialViewContext extends PartialViewContextWrapper {
    private javax.faces.context.PartialViewContext wrapped;
    private static final String PORTION_DATAS_KEY = PartialViewContext.class.getName() + ".ajaxPortionDatas";
    private static final String SESSION_EXPIRATION_EXTENSION_KEY = PartialViewContext.class.getName() + ".sessionExpirationExtension";
    private static final String APPLICATION_SESSION_EXPIRATION_PARAM_NAME = "org.openfaces.ajax.sessionExpiration";
    private static final String SESSION_EXPIRATION_HANDLING_SILENT = "silent";
    private static final String SESSION_EXPIRATION_HANDLING_DEFAULT = "default";
    private static final Pattern JS_VAR_PATTERN = Pattern.compile("\\bvar\\b");
    private static final String PARAM_ACTION_COMPONENT = "_of_actionComponent";
    private static final String PARAM_ACTION_LISTENER = "_of_actionListener";
    private static final String PARAM_ACTION = "_of_action";
    private static final String PARAM_IMMEDIATE = "_of_immediate";

    public PartialViewContext(javax.faces.context.PartialViewContext wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public PartialResponseWriter getPartialResponseWriter() {
        PartialResponseWriter originalWriter = super.getPartialResponseWriter();
        return new PartialResponseWriterWrapper(originalWriter) {
            @Override
            public void startUpdate(String targetId) throws IOException {
                if (targetId.equals(PartialResponseWriter.VIEW_STATE_MARKER)) {
                    FacesContext context = FacesContext.getCurrentInstance();
                    prepareViewExpirationExtension(context);
                    if (!ViewExpiredExceptionHandler.isExpiredView(context))
                        prepareAjaxPortions(context);
                }
                super.startUpdate(targetId);
            }

            @Override
            public void startExtension(Map<String, String> attributes) throws IOException {
                renderAdditionalResponseIfNeeded();
                super.startExtension(attributes);
            }

            @Override
            public void endDocument() throws IOException {
                renderAdditionalResponseIfNeeded();
                super.endDocument();
            }

            private void renderAdditionalResponseIfNeeded() {
                FacesContext context = FacesContext.getCurrentInstance();
                Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
                String additionalResponseRenderedKey = PartialViewContext.class.getName() + ".additionalResponseRendered";
                if (requestMap.containsKey(additionalResponseRenderedKey))
                    return;

                requestMap.put(additionalResponseRenderedKey, true);
                
                renderAdditionalPartialResponse();
            }
        };
    }

    @Override
    public javax.faces.context.PartialViewContext getWrapped() {
        return wrapped;
    }

    @Override
    public void setPartialRequest(boolean isPartialRequest) {
        wrapped.setPartialRequest(isPartialRequest);
    }

    @Override
    public void processPartial(PhaseId phaseId) {
        super.processPartial(phaseId);
        if (isAjaxRequest()) {
            if (phaseId == PhaseId.UPDATE_MODEL_VALUES) {
                processAjaxExecutePhase(FacesContext.getCurrentInstance());
            }
        }

    }

    @Override
    public boolean isRenderAll() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (AjaxUtil.isAjaxPortionRequest(context))
            return false;
        return super.isRenderAll();
    }

    @Override
    public Collection<String> getExecuteIds() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (ViewExpiredExceptionHandler.isExpiredView(context))
            return Collections.emptyList();

        return super.getExecuteIds();
    }

    @Override
    public Collection<String> getRenderIds() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (AjaxUtil.isAjaxPortionRequest(context) || ViewExpiredExceptionHandler.isExpiredView(context))
            return Collections.emptyList();
        Set<String> result = new LinkedHashSet<String>(super.getRenderIds());
        result.addAll(AjaxRequest.getInstance().getReloadedComponentIds());
        List<String> additionalComponents = new ArrayList<String>();
        UIViewRoot viewRoot = context.getViewRoot();
        for (String id : result) {
            UIComponent component = viewRoot.findComponent(id);
            if (component == null) continue;
            if (!(component instanceof ComponentWithExternalParts)) continue;
            Collection<String> externalPartIds = ((ComponentWithExternalParts) component).getExternalPartIds();
            String componentClientId = component.getClientId(context);
            List<String> unprocessableIds = new ArrayList<String>();
            for (String externalPartId : externalPartIds) {
                // component's facets such as DataTable's "above" and "below" components are included into the
                // "external components" (as returned by getExternalPartIds), but these components are skipped by the
                // table's visitTree method and thus these facets don't get automatically included into the rerendered
                // component list, so we need to collect them and render their "update" portions manually here.
                if (externalPartId.startsWith(componentClientId)) {
                    unprocessableIds.add(externalPartId);
                }
            }
            context.getExternalContext().getRequestMap().put(getUnprocessableComponentIdsKey(), unprocessableIds);
            additionalComponents.addAll(externalPartIds);
        }

        result.addAll(additionalComponents);
        return result;
    }

    private String getUnprocessableComponentIdsKey() {
        return PartialViewContext.class.getName() + ".unprocessableComponentIds";
    }

    public Collection<String> getRenderIdsNotRenderedYet(FacesContext context) {
        Map<String, Object> map = context.getExternalContext().getRequestMap();
        return (Collection<String>) map.get(getUnprocessableComponentIdsKey());
    }

    private void writeComponentUpdate(FacesContext context, UIComponent component) throws IOException {
        PartialResponseWriter writer = getPartialResponseWriter();
        writer.startUpdate(component.getClientId(context));
        component.encodeAll(context);
        writer.endUpdate();
    }

    public void renderAdditionalPartialResponse() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            renderAdditionalPartialResponse(context);
        } catch (IOException e) {
            throw new FacesException(e);
        }
    }

    private void renderAdditionalPartialResponse(FacesContext context) throws IOException {
        Collection<String> additionalUpdateList = getRenderIdsNotRenderedYet(context);
        UIViewRoot viewRoot = context.getViewRoot();
        if (additionalUpdateList != null)
            for (String componentId : additionalUpdateList) {
                UIComponent component = viewRoot.findComponent(componentId);
                if (component == null) continue;
                writeComponentUpdate(context, component);
            }
        renderAjaxPortions(context);
        renderAjaxInitScripts(context);
        renderAjaxResult(context);
    }

    private void prepareViewExpirationExtension(final FacesContext context) throws IOException {
        if (!ViewExpiredExceptionHandler.isExpiredView(context)) return;
        final ExternalContext externalContext = context.getExternalContext();
        final AjaxExtension extension = prepareExtension(context, "sessionExpiration", "", new ExtensionRenderer() {
            public JSONObject render() throws IOException {
                AtomicReference<RequestFacade> requestFacade = new AtomicReference<RequestFacade>(RequestFacade.getInstance(externalContext.getRequest()));
                handleSessionExpirationOnEncodeChildren(context, requestFacade.get());
                return null;
            }
        }, true);

        externalContext.getRequestMap().put(SESSION_EXPIRATION_EXTENSION_KEY, extension);

    }

    private static List<String> prepareResourceUrls(Collection<String> resources) {
        FacesContext context = FacesContext.getCurrentInstance();
        List<String> result = new ArrayList<String>(resources.size());
        for (String resource : resources) {
            result.add(Resources.getInternalURL(context, resource));
        }
        return result;
    }

    private static void renderAjaxInitScripts(FacesContext context) throws IOException {
        InitScript script = getCombinedAjaxInitScripts(context);
        if (script == null) return;

        javax.faces.context.PartialViewContext partialViewContext = context.getPartialViewContext();
        PartialResponseWriter partialWriter = partialViewContext.getPartialResponseWriter();
        partialWriter.startEval();
        partialWriter.write(
                new FunctionCallScript("O$.Ajax._runScript", script.getScript(), script.getJsFiles()).toString()
        );
        partialWriter.endEval();
    }

    public static InitScript getCombinedAjaxInitScripts(FacesContext context) {
        ScriptBuilder sb = new ScriptBuilder();
        Set<String> jsFiles = new LinkedHashSet<String>();
        List<InitScript> initScripts = Rendering.getAjaxInitScripts(context);
        if (initScripts.isEmpty()) return null;
        boolean semicolonNeeded = false;
        for (InitScript initScript : initScripts) {
            Script script = initScript.getScript();
            if (semicolonNeeded) sb.semicolon();
            sb.append(script);
            semicolonNeeded = true;
            String[] files = initScript.getJsFiles();
            if (files != null)
                jsFiles.addAll(Arrays.asList(files));
        }
        initScripts.clear();
        // remove the possible null references, which are normally allowed to present, from js library list
        jsFiles.remove(null);
        InitScript script = new InitScript(new AnonymousFunction(sb), jsFiles.toArray(new String[jsFiles.size()]));
        return script;
    }

    public static void handleSessionExpirationOnEncodeChildren(FacesContext context, RequestFacade request) throws IOException {
        ExternalContext externalContext = context.getExternalContext();

        UIViewRoot viewRoot = context.getViewRoot();
        List<UIComponent> children = viewRoot.getChildren();
        AjaxSettings ajaxSettings = null;

        Collection<String> componentIds = context.getPartialViewContext().getRenderIds();

        assertChildren(viewRoot);

        UIComponent component = componentIds != null && componentIds.size() > 0 ? findComponentById(viewRoot, componentIds.iterator().next(), false, false) : null;
        if (component != null && component.getChildCount() > 0) {
            List<UIComponent> ajaxSubmittedComponentChildren = component.getChildren();
            ajaxSettings = findAjaxSettings(ajaxSubmittedComponentChildren);
        }

        if (ajaxSettings == null) {
            ajaxSettings = findPageAjaxSettings(children);
        }

        if (ajaxSettings == null) {
            Map initParameterMap = externalContext.getInitParameterMap();
            String sessionExpirationHandling = (initParameterMap != null)
                    ? (String) initParameterMap.get(APPLICATION_SESSION_EXPIRATION_PARAM_NAME)
                    : null;

            if (sessionExpirationHandling != null && sessionExpirationHandling.length() > 0) {
                if (sessionExpirationHandling.equalsIgnoreCase(SESSION_EXPIRATION_HANDLING_SILENT)) {
                    ajaxSettings = createSilentSessionExpirationSettings();
                } else if (sessionExpirationHandling.equalsIgnoreCase(SESSION_EXPIRATION_HANDLING_DEFAULT)) {
                    ajaxSettings = createDefaultSessionExpirationSettings(context);
                }
            } else {
                ajaxSettings = createDefaultSessionExpirationSettings(context);
            }
        }

        //noinspection ConstantConditions
        ajaxSettings.encodeAll(context);
    }

    static AjaxSettings findAjaxSettings(List<UIComponent> children) {
        AjaxSettings result = null;
        for (Object iteratedChild : children) {
            if (iteratedChild instanceof AjaxSettings) {
                result = (AjaxSettings) iteratedChild;
                return result;
            }
            UIComponent uiComponent = (UIComponent) iteratedChild;
            if (uiComponent.getChildCount() > 0) {
                result = findAjaxSettings(uiComponent.getChildren());
                if (result != null) {
                    return result;
                }
            }
        }
        return result;
    }

    static AjaxSettings findPageAjaxSettings(List<UIComponent> children) {
        AjaxSettings result = null;
        for (Object iteratedChild : children) {
            if (iteratedChild instanceof AjaxSettings && isPageSettings((AjaxSettings) iteratedChild)) {
                result = (AjaxSettings) iteratedChild;
                return result;
            }
            UIComponent uiComponent = (UIComponent) iteratedChild;
            if (uiComponent.getChildCount() > 0) {
                result = findPageAjaxSettings(uiComponent.getChildren());
                if (result != null) {
                    return result;
                }
            }
        }
        return result;
    }

    static boolean isPageSettings(AjaxSettings ajaxSettings) {
        return (ajaxSettings.getParent() instanceof UIViewRoot || ajaxSettings.getParent() instanceof UIForm);
    }

    static AjaxSettings createSilentSessionExpirationSettings() {
        AjaxSettings result = new AjaxSettings();
        result.setSessionExpiration(new SilentSessionExpiration());
        return result;
    }

    static AjaxSettings createDefaultSessionExpirationSettings(FacesContext context) {
        AjaxSettings result = new AjaxSettings();
        DefaultSessionExpiration dse = new DefaultSessionExpiration();
        result.setSessionExpiration(dse);
        return result;
    }

    static UIComponent findComponentById(UIComponent parent,
                                                 String id,
                                                 boolean preProcessDecodesOnTables,
                                                 boolean preRenderResponseOnTables) {
        return findComponentById(parent, id, preProcessDecodesOnTables, preRenderResponseOnTables, true);
    }

    static void extractScripts(StringBuilder buffer,
                               StringBuilder rawScriptBuffer,
                               StringBuilder rtLibraryScriptBuffer) {
        String scriptStart = "<script";
        String scriptEnd = "/script>";
        while (true) {
            int fromIndex = buffer.indexOf(scriptStart);
            if (fromIndex == -1)
                break;

            int toIndex = buffer.indexOf(scriptEnd, fromIndex);
            if (toIndex == -1)
                break;

            toIndex += scriptEnd.length();
            String rawScript = buffer.substring(fromIndex, toIndex);
            String script = purifyScripts(rawScript);

            Matcher matcher = JS_VAR_PATTERN.matcher(rawScript);
            boolean varFound = false;//matcher.find();

            buffer.delete(fromIndex, toIndex);

            if (varFound) {
                rtLibraryScriptBuffer.append(script).append("\n");
            } else {
                rawScriptBuffer.append(rawScript);
            }
        }
    }

    private static String purifyScripts(String script) {
        StringBuffer result = new StringBuffer();
        int startIdx = script.indexOf("<script");
        int endIdx = script.indexOf("</script>");
        if (startIdx == -1 || endIdx == -1) return script;
        int endScriptInit = script.indexOf(">", startIdx + 1);
        if (startIdx > 0) {
            result.append(script.substring(0, startIdx));
            result.append("\n");
            script = script.substring(startIdx);
            // re-read indexes
            startIdx = script.indexOf("<script");
            endIdx = script.indexOf("</script>");
            if (startIdx != -1)
                endScriptInit = script.indexOf(">", startIdx + 1);
        }
        if (endScriptInit == -1) return script;
        while (startIdx > -1) {
            result.append(script.substring(endScriptInit + 1, endIdx));
            result.append("\n");
            script = script.substring(endIdx + "</script>".length());
            // re-read indexes
            startIdx = script.indexOf("<script");
            endIdx = script.indexOf("</script>");
            if (startIdx > -1)
                endScriptInit = script.indexOf(">", startIdx + 1);
        }
        if (script.length() > 0) {
            result.append(script);
            result.append("\n");
        }
        return result.toString();
    }

    public static ResponseWriter substituteResponseWriter(FacesContext context, Writer innerWriter) {
        ResponseWriter newWriter;
        ResponseWriter responseWriter = context.getResponseWriter();
        if (responseWriter != null) {
            newWriter = responseWriter.cloneWithWriter(innerWriter);
        } else {
            RenderKitFactory factory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
            RenderKit renderKit = factory.getRenderKit(context, context.getViewRoot().getRenderKitId());
            newWriter = renderKit.createResponseWriter(innerWriter, null, context.getExternalContext().getRequestCharacterEncoding());
        }
        context.setResponseWriter(newWriter);
        return responseWriter;
    }

    public static void restoreWriter(FacesContext context, ResponseWriter originalWriter) {
        if (originalWriter != null)
            context.setResponseWriter(originalWriter);
    }

    protected static void assertChildren(UIViewRoot viewRoot) {
        if (viewRoot.getChildCount() == 0) {
            throw new IllegalStateException("View should have been already restored.");
        }
    }

    public static String processAjaxExecutePhase(FacesContext context) {
        UIViewRoot viewRoot = context.getViewRoot();
        Map<String, String> requestParams = context.getExternalContext().getRequestParameterMap();
        String listener = requestParams.get(PARAM_ACTION_LISTENER);
        String action = requestParams.get(PARAM_ACTION);
        String actionComponentId = requestParams.get(PARAM_ACTION_COMPONENT);
        Log.log(context, "try invoke listener");
        if (listener != null || action != null) {
            ELContext elContext = context.getELContext();
            UIComponent component = null;
            if (actionComponentId != null)
                component = findComponentById(viewRoot, actionComponentId, true, false, false);
            if (component == null)
                component = viewRoot;

            Object result = null;
            if (action != null) {
                MethodExpression methodBinding = context.getApplication().getExpressionFactory().createMethodExpression(
                        elContext, "#{" + action + "}", String.class, new Class[]{});
                /*result = */
                methodBinding.invoke(elContext, null);
            }
            if (listener != null) {
                AjaxActionEvent event = new AjaxActionEvent(component);
                event.setPhaseId(Boolean.valueOf(requestParams.get(PARAM_IMMEDIATE)) ? PhaseId.APPLY_REQUEST_VALUES : PhaseId.INVOKE_APPLICATION);
                MethodExpression methodExpression = context.getApplication().getExpressionFactory().createMethodExpression(
                        elContext, "#{" + listener + "}", void.class, new Class[]{ActionEvent.class});
                try {
                    methodExpression.getMethodInfo(elContext);
                } catch (MethodNotFoundException e) {
                    // both actionEvent and AjaxActionEvent parameter declarations are allowed
                    methodExpression = context.getApplication().getExpressionFactory().createMethodExpression(
                            elContext, "#{" + listener + "}", void.class, new Class[]{AjaxActionEvent.class});
                }
                methodExpression.invoke(elContext, new Object[]{event});
                Object listenerResult = event.getAjaxResult();
                if (listenerResult != null)
                    result = listenerResult;
            }
            if (result != null)
                AjaxRequest.getInstance().setAjaxResult(result);
        }
        return actionComponentId;
    }

    public static UIComponent findComponentById(UIComponent parent,
                                                String id,
                                                boolean preProcessDecodesOnTables,
                                                boolean preRenderResponseOnTables,
                                                boolean checkComponentPresence) {
        UIComponent componentByPath = findComponentByPath(parent, id, preProcessDecodesOnTables, preRenderResponseOnTables);
        if (checkComponentPresence && componentByPath == null)
            throw new FacesException("Component by id not found: " + id);
        return componentByPath;
    }

    private static UIComponent findComponentByPath(UIComponent parent,
                                                   String path,
                                                   boolean preProcessDecodesOnTables,
                                                   boolean preRenderResponseOnTables) {
        while (true) {
            if (path == null) {
                return null;
            }

            int separator = path.indexOf(NamingContainer.SEPARATOR_CHAR, 1);
            if (separator == -1)
                return componentById(parent, path, true, preProcessDecodesOnTables, preRenderResponseOnTables);

            String id = path.substring(0, separator);
            UIComponent nextParent = componentById(parent, id, false, preProcessDecodesOnTables, preRenderResponseOnTables);
            if (nextParent == null) {
                return null;
            }
            parent = nextParent;
            path = path.substring(separator + 1);
        }
    }

    private static UIComponent componentById(UIComponent parent, String id, boolean isLastComponentInPath,
                                             boolean preProcessDecodesOnTables, boolean preRenderResponseOnTables) {
        if (id.length() > 0 && (isNumberBasedId(id) || id.startsWith(":")) && parent instanceof AbstractTable) {
            AbstractTable table = ((AbstractTable) parent);
            if (!isLastComponentInPath) {
                if (preProcessDecodesOnTables)
                    table.invokeBeforeProcessDecodes(FacesContext.getCurrentInstance());
                if (preRenderResponseOnTables) {
                    table.invokeBeforeRenderResponse(FacesContext.getCurrentInstance());
                    table.setRowIndex(-1); // make the succeeding setRowIndex call provide the just-read actual row data through request-scope variables
                }

                int rowIndex = table.getRowIndexByClientSuffix(id);
                if (table.getRowIndex() == rowIndex) {
                    // ensure row index setting will be run anew to ensure proper request-scope variable values
                    table.setRowIndex(-1);
                }
                table.setRowIndex(rowIndex);
            } else {
                int rowIndex = table.getRowIndexByClientSuffix(id);
                if (table.getRowIndex() == rowIndex) {
                    // ensure row index setting will be run anew to ensure proper request-scope variable values
                    table.setRowIndex(-1);
                }
                table.setRowIndex(rowIndex);
            }
            return table;
        } else if (isNumberBasedId(id) && parent instanceof UIData) {
            UIData grid = ((UIData) parent);
            int rowIndex = Integer.parseInt(id);
            grid.setRowIndex(rowIndex);
            return grid;
        } else if (id.charAt(0) == ':' && parent instanceof OUIObjectIterator) {
            id = id.substring(1);
            OUIObjectIterator iterator = (OUIObjectIterator) parent;
            iterator.setObjectId(id);
            return (UIComponent) iterator;
        } else if (isNumberBasedId(id)) {
            try {
                Class clazz = Class.forName("com.sun.facelets.component.UIRepeat");
                if (clazz.isInstance(parent)) {
                    ReflectionUtil.invokeMethod("com.sun.facelets.component.UIRepeat", "setIndex",
                            new Class[]{Integer.TYPE}, new Object[]{Integer.parseInt(id)}, parent);
                    return parent;
                }
            } catch (ClassNotFoundException e) {
                //do nothing - it's ok - not facelets environment
            }

        }
        if (id.equals(parent.getId()))
            return parent;

        Iterator<UIComponent> iterator = parent.getFacetsAndChildren();
        while (iterator.hasNext()) {
            UIComponent child = iterator.next();
            if (child instanceof NamingContainer) {
                if (id.equals(child.getId()))
                    return child;
            } else {
                UIComponent component = componentById(child, id,
                        isLastComponentInPath, preProcessDecodesOnTables, preRenderResponseOnTables);
                if (component != null)
                    return component;
            }
        }
        return null;
    }

    private static boolean isNumberBasedId(String id) {
        if (id == null || id.length() == 0)
            return false;

        char c = id.charAt(0);
        return Character.isDigit(c);
    }

    private interface ExtensionRenderer {
        public JSONObject render() throws IOException;
    }

    private static void prepareAjaxPortions(final FacesContext context) throws IOException {
        List<String> updatePortions = AjaxUtil.getAjaxPortionNames(context);
        if (updatePortions.isEmpty()) return;
        ExternalContext externalContext = context.getExternalContext();
        String renderParam = externalContext.getRequestParameterMap().get(
                javax.faces.context.PartialViewContext.PARTIAL_RENDER_PARAM_NAME);
        String[] renderIds = renderParam.split("[ \t]+");
        if (renderIds.length != 1)
            throw new RuntimeException("There should be one target component but was: " + renderIds.length);
        final UIComponent component = findComponentById(context.getViewRoot(), renderIds[0],
                false, true, true);

        RenderKitFactory factory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
        RenderKit renderKit = factory.getRenderKit(context, context.getViewRoot().getRenderKitId());
        Renderer renderer = renderKit.getRenderer(component.getFamily(), component.getRendererType());
        final JSONObject customJSONParam = AjaxUtil.getCustomJSONParam(context);
        final AjaxPortionRenderer ajaxComponentRenderer = (AjaxPortionRenderer) renderer;
        List<AjaxExtension> extensions = new ArrayList<AjaxExtension>();
        for (final String portionName : updatePortions) {
            AjaxExtension extension = prepareExtension(context, "portionData", portionName, new ExtensionRenderer() {
                public JSONObject render() throws IOException {
                    try {
                        return ajaxComponentRenderer.encodeAjaxPortion(context, component, portionName, customJSONParam);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, false);
            extensions.add(extension);
        }
        context.getExternalContext().getRequestMap().put(PORTION_DATAS_KEY, extensions);
    }

    private static AjaxExtension prepareExtension(FacesContext context,
                                                  String extensionType,
                                                  String portionName,
                                                  ExtensionRenderer extensionRenderer,
                                                  boolean appendAjaxInitScripts) throws IOException {
        StringBuilder portionOutput;
        JSONObject responseData;
        StringWriter stringWriter = new StringWriter();
        ResponseWriter originalWriter = substituteResponseWriter(context, stringWriter);
        try {
            responseData = extensionRenderer.render();
            portionOutput = new StringBuilder(stringWriter.toString());
        } finally {
            restoreWriter(context, originalWriter);
        }

        StringBuilder rawScriptsBuffer = new StringBuilder();
        StringBuilder rtLibraryScriptsBuffer = new StringBuilder();
        extractScripts(portionOutput, rawScriptsBuffer, rtLibraryScriptsBuffer);

        Collection<String> libraries = new LinkedHashSet<String>();
        List<String> registeredJsLibs = Resources.getRegisteredJsLibraries();
        if (registeredJsLibs != null)
            libraries.addAll(registeredJsLibs);

        if (appendAjaxInitScripts) {
            InitScript script = PartialViewContext.getCombinedAjaxInitScripts(context);
            if (script != null) {
                libraries.addAll(Arrays.asList(script.getJsFiles()));
                Script additionalScript = new ScriptBuilder("_temp_=").append(script.getScript()).append("();");
                rawScriptsBuffer.append(additionalScript);
            }
        }

        AjaxExtension extension = new AjaxExtension(
                extensionType,
                portionName,
                portionOutput.toString(),
                responseData,
                libraries,
                rawScriptsBuffer.toString());
        return extension;
    }

    private static void renderAjaxPortions(FacesContext context) throws IOException {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        AjaxExtension sessionExpirationExtension = (AjaxExtension) requestMap.get(SESSION_EXPIRATION_EXTENSION_KEY);
        javax.faces.context.PartialViewContext partialViewContext = context.getPartialViewContext();
        PartialResponseWriter partialWriter = partialViewContext.getPartialResponseWriter();
        if (sessionExpirationExtension != null) {
            sessionExpirationExtension.render(partialWriter);
            return;
        }
        List<AjaxExtension> extensions = (List<AjaxExtension>) requestMap.get(PORTION_DATAS_KEY);
        if (extensions != null)
            for (AjaxExtension extension : extensions) {
                extension.render(partialWriter);
            }
    }

    private static class AjaxExtension {
        private String type;
        private String portionName;
        private String portionOutput;
        private JSONObject responseData;
        private Collection<String> registeredJsLibraries;
        private String scripts;

        private AjaxExtension(String type, String portionName, String portionOutput, JSONObject responseData, Collection<String> registeredJsLibraries, String scripts) {
            this.type = type;
            this.portionName = portionName;
            this.portionOutput = portionOutput;
            this.responseData = responseData;
            this.registeredJsLibraries = registeredJsLibraries;
            this.scripts = scripts;
        }

        public void render(PartialResponseWriter writer) throws IOException {
            Map<String, String> extensionAttributes = new HashMap<String, String>();
            extensionAttributes.put("ln", "openfaces");
            extensionAttributes.put("type", type);
            extensionAttributes.put("portion", portionName);
            extensionAttributes.put("text", portionOutput);
            extensionAttributes.put("data", responseData != null ? responseData.toString() : "null");
            extensionAttributes.put("jsLibs", new JSONArray(registeredJsLibraries).toString());
            extensionAttributes.put("scripts", scripts);

            writer.startExtension(extensionAttributes);
            writer.endExtension();
        }

    }

    private static void renderAjaxResult(FacesContext context) throws IOException {
        javax.faces.context.PartialViewContext partialViewContext = context.getPartialViewContext();
        PartialResponseWriter partialWriter = partialViewContext.getPartialResponseWriter();
        Map<String, String> extensionAttributes = new HashMap<String, String>();
        extensionAttributes.put("ln", "openfaces");
        extensionAttributes.put("type", "ajaxResult");
        Object ajaxResult = AjaxRequest.getInstance(context).getAjaxResult();
        extensionAttributes.put("ajaxResult", resultValueToJsValue(ajaxResult));

        partialWriter.startExtension(extensionAttributes);
        partialWriter.endExtension();
    }

    private static String resultValueToJsValue(Object resultValue) {
        if (resultValue != null && resultValue.getClass().isArray()) {
            List<Object> resultAsList = new ArrayList<Object>();
            for (int i = 0, count = Array.getLength(resultValue); i < count; i++) {
                resultAsList.add(Array.get(resultValue, i));
            }
            resultValue = resultAsList;
        }
        String value;
        if (resultValue == null)
            value = "null";
        else if (resultValue instanceof String)
            value = "\"" + resultValue + "\"";
        else if (resultValue instanceof JSONString)
            value = ((JSONString) resultValue).toJSONString();
        else if (resultValue instanceof Iterable) {
            StringBuilder sb = new StringBuilder("[");
            for (Object entry : (Iterable) resultValue) {
                if (sb.length() > 1) sb.append(",");
                sb.append(resultValueToJsValue(entry));
            }
            sb.append("]");
            value = sb.toString();
        } else if (resultValue instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            Set<Map.Entry> entries = ((Map) resultValue).entrySet();
            for (Map.Entry entry : entries) {
                if (sb.length() > 1) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":").append(resultValueToJsValue(entry.getValue()));
            }
            sb.append("}");
            value = sb.toString();
        } else
            value = resultValue.toString();
        return value;
    }


}
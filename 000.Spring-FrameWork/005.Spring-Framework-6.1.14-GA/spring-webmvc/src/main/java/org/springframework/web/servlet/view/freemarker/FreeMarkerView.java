/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.view.freemarker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.AbstractTemplateView;

/**
 * View using the FreeMarker template engine.
 *
 * <p>Exposes the following configuration properties:
 * <ul>
 * <li><b>{@link #setUrl(String) url}</b>: the location of the FreeMarker template
 * relative to the FreeMarker template context (directory).</li>
 * <li><b>{@link #setEncoding(String) encoding}</b>: the encoding used to decode
 * byte sequences to character sequences when reading the FreeMarker template file.
 * Default is determined by the FreeMarker {@link Configuration}.</li>
 * <li><b>{@link #setContentType(String) contentType}</b>: the content type of the
 * rendered response. Defaults to {@code "text/html;charset=ISO-8859-1"} but should
 * typically be set to a value that corresponds to the actual generated content
 * type (see note below).</li>
 * </ul>
 *
 * <p>Depends on a single {@link FreeMarkerConfig} object such as
 * {@link FreeMarkerConfigurer} being accessible in the current web application
 * context. Alternatively the FreeMarker {@link Configuration} can be set directly
 * via {@link #setConfiguration}.
 *
 * <p><b>Note:</b> To ensure that the correct encoding is used when rendering the
 * response, set the {@linkplain #setContentType(String) content type} with an
 * appropriate {@code charset} attribute &mdash; for example,
 * {@code "text/html;charset=UTF-8"}. When using {@link FreeMarkerViewResolver}
 * to create the view for you, set the
 * {@linkplain FreeMarkerViewResolver#setContentType(String) content type}
 * directly in the {@code FreeMarkerViewResolver}.
 *
 * <p>Note: Spring's FreeMarker support requires FreeMarker 2.3.21 or higher.
 * As of Spring Framework 6.0, FreeMarker templates are rendered in a minimal
 * fashion without JSP support, just exposing request attributes in addition
 * to the MVC-provided model map for alignment with common Servlet resources.
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 03.03.2004
 * @see #setUrl
 * @see #setExposeSpringMacroHelpers
 * @see #setEncoding
 * @see #setConfiguration
 * @see FreeMarkerConfig
 * @see FreeMarkerConfigurer
 */
public class FreeMarkerView extends AbstractTemplateView {

	@Nullable
	private String encoding;

	@Nullable
	private Configuration configuration;


	/**
	 * Set the encoding used to decode byte sequences to character sequences when
	 * reading the FreeMarker template file for this view.
	 * <p>Defaults to {@code null} to signal that the FreeMarker
	 * {@link Configuration} should be used to determine the encoding.
	 * <p>A non-null encoding will override the default encoding determined by
	 * the FreeMarker {@code Configuration}.
	 * <p>If the encoding is not explicitly set here or in the FreeMarker
	 * {@code Configuration}, FreeMarker will read template files using the platform
	 * file encoding (defined by the JVM system property {@code file.encoding})
	 * or {@code "utf-8"} if the platform file encoding is undefined.
	 * <p>It's recommended to specify the encoding in the FreeMarker {@code Configuration}
	 * rather than per template if all your templates share a common encoding.
	 * <p>Note that the specified or default encoding is not used for template
	 * rendering. Instead, an explicit encoding must be specified for the rendering
	 * process. See the note in the {@linkplain FreeMarkerView class-level
	 * documentation} for details.
	 * @see freemarker.template.Configuration#setDefaultEncoding
	 * @see #getEncoding()
	 * @see #setContentType(String)
	 */
	public void setEncoding(@Nullable String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Get the encoding used to decode byte sequences to character sequences
	 * when reading the FreeMarker template file for this view, or {@code null}
	 * to signal that the FreeMarker {@link Configuration} should be used to
	 * determine the encoding.
	 * @see #setEncoding(String)
	 */
	@Nullable
	protected String getEncoding() {
		return this.encoding;
	}

	/**
	 * Set the FreeMarker {@link Configuration} to be used by this view.
	 * <p>If not set, the default lookup will occur: a single {@link FreeMarkerConfig}
	 * is expected in the current web application context, with any bean name.
	 */
	public void setConfiguration(@Nullable Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Return the FreeMarker {@link Configuration} used by this view.
	 */
	@Nullable
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * Obtain the FreeMarker {@link Configuration} for actual use.
	 * @return the FreeMarker configuration (never {@code null})
	 * @throws IllegalStateException in case of no Configuration object set
	 * @since 5.0
	 */
	protected Configuration obtainConfiguration() {
		Configuration configuration = getConfiguration();
		Assert.state(configuration != null, "No Configuration set");
		return configuration;
	}


	/**
	 * Invoked on startup. Looks for a single {@link FreeMarkerConfig} bean to
	 * find the relevant {@link Configuration} for this view.
	 * <p>Checks that the template for the default Locale can be found:
	 * FreeMarker will check non-Locale-specific templates if a
	 * locale-specific one is not found.
	 * @see freemarker.cache.TemplateCache#getTemplate
	 */
	@Override
	protected void initServletContext(ServletContext servletContext) throws BeansException {
		if (getConfiguration() == null) {
			FreeMarkerConfig config = autodetectConfiguration();
			setConfiguration(config.getConfiguration());
		}
	}

	/**
	 * Autodetect a {@link FreeMarkerConfig} object via the {@code ApplicationContext}.
	 * @return the {@code FreeMarkerConfig} instance to use for FreeMarkerViews
	 * @throws BeansException if no {@link FreeMarkerConfig} bean could be found
	 * @see #getApplicationContext
	 * @see #setConfiguration
	 */
	protected FreeMarkerConfig autodetectConfiguration() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					obtainApplicationContext(), FreeMarkerConfig.class, true, false);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException(
					"Must define a single FreeMarkerConfig bean in this web application context " +
					"(may be inherited): FreeMarkerConfigurer is the usual implementation. " +
					"This bean may be given any name.", ex);
		}
	}

	/**
	 * Return the configured FreeMarker {@link ObjectWrapper}, or the
	 * {@linkplain ObjectWrapper#DEFAULT_WRAPPER default wrapper} if none specified.
	 * @see freemarker.template.Configuration#getObjectWrapper()
	 */
	protected ObjectWrapper getObjectWrapper() {
		ObjectWrapper ow = obtainConfiguration().getObjectWrapper();
		return (ow != null ? ow :
				new DefaultObjectWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build());
	}

	/**
	 * Check that the FreeMarker template used for this view exists and is valid.
	 * <p>Can be overridden to customize the behavior, for example in case of
	 * multiple templates to be rendered into a single view.
	 */
	@Override
	public boolean checkResource(Locale locale) throws Exception {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");

		try {
			// Check that we can get the template, even if we might subsequently get it again.
			getTemplate(url, locale);
			return true;
		}
		catch (FileNotFoundException ex) {
			// Allow for ViewResolver chaining...
			return false;
		}
		catch (ParseException ex) {
			throw new ApplicationContextException("Failed to parse [" + url + "]", ex);
		}
		catch (IOException ex) {
			throw new ApplicationContextException("Failed to load [" + url + "]", ex);
		}
	}


	/**
	 * Process the model map by merging it with the FreeMarker template.
	 * <p>Output is directed to the servlet response.
	 * <p>This method can be overridden if custom behavior is needed.
	 */
	@Override
	protected void renderMergedTemplateModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		exposeHelpers(model, request);
		doRender(model, request, response);
	}

	/**
	 * Expose helpers unique to each rendering operation. This is necessary so that
	 * different rendering operations can't overwrite each other's formats etc.
	 * <p>Called by {@code renderMergedTemplateModel}. The default implementation
	 * is empty. This method can be overridden to add custom helpers to the model.
	 * @param model the model that will be passed to the template at merge time
	 * @param request current HTTP request
	 * @throws Exception if there's a fatal error while we're adding information to the context
	 * @see #renderMergedTemplateModel
	 */
	protected void exposeHelpers(Map<String, Object> model, HttpServletRequest request) throws Exception {
	}

	/**
	 * Render the FreeMarker view to the given response, using the given model
	 * map which contains the complete template model to use.
	 * <p>The default implementation renders the template specified by the "url"
	 * bean property, retrieved via {@code getTemplate}. It delegates to the
	 * {@code processTemplate} method to merge the template instance with
	 * the given template model.
	 * <p>Can be overridden to customize the behavior, for example to render
	 * multiple templates into a single view.
	 * @param model the model to use for rendering
	 * @param request current HTTP request
	 * @param response current servlet response
	 * @throws IOException if the template file could not be retrieved
	 * @throws Exception if rendering failed
	 * @see #setUrl
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 * @see #getTemplate(java.util.Locale)
	 * @see #processTemplate
	 * @see freemarker.ext.servlet.FreemarkerServlet
	 */
	protected void doRender(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		// Expose model to JSP tags (as request attributes).
		exposeModelAsRequestAttributes(model, request);
		// Expose FreeMarker hash model.
		SimpleHash fmModel = buildTemplateModel(model, request, response);

		// Grab the locale-specific version of the template.
		Locale locale = RequestContextUtils.getLocale(request);
		processTemplate(getTemplate(locale), fmModel, response);
	}

	/**
	 * Build a FreeMarker template model for the given model Map.
	 * <p>The default implementation builds a {@link SimpleHash} for the
	 * given MVC model with an additional fallback to request attributes.
	 * @param model the model to use for rendering
	 * @param request current HTTP request
	 * @param response current servlet response
	 * @return the FreeMarker template model, as a {@link SimpleHash} or subclass thereof
	 */
	protected SimpleHash buildTemplateModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) {

		SimpleHash fmModel = new RequestHashModel(getObjectWrapper(), request);
		fmModel.putAll(model);
		return fmModel;
	}

	/**
	 * Retrieve the FreeMarker {@link Template} for the given locale, to be
	 * rendered by this view.
	 * <p>By default, the template specified by the "url" bean property
	 * will be retrieved.
	 * @param locale the current locale
	 * @return the FreeMarker {@code Template} to render
	 * @throws IOException if the template file could not be retrieved
	 * @see #setUrl
	 * @see #getTemplate(String, java.util.Locale)
	 */
	protected Template getTemplate(Locale locale) throws IOException {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		return getTemplate(url, locale);
	}

	/**
	 * Retrieve the FreeMarker {@link Template} for the specified name and locale,
	 * using the {@linkplain #setEncoding(String) configured encoding} if set.
	 * <p>Can be called by subclasses to retrieve a specific template,
	 * for example to render multiple templates into a single view.
	 * @param name the file name of the desired template
	 * @param locale the current locale
	 * @return the FreeMarker template
	 * @throws IOException if the template file could not be retrieved
	 * @see #setEncoding(String)
	 */
	protected Template getTemplate(String name, Locale locale) throws IOException {
		return (getEncoding() != null ?
				obtainConfiguration().getTemplate(name, locale, getEncoding()) :
				obtainConfiguration().getTemplate(name, locale));
	}

	/**
	 * Process the FreeMarker template to the servlet response.
	 * <p>Can be overridden to customize the behavior.
	 * @param template the template to process
	 * @param model the model for the template
	 * @param response servlet response (use this to get the OutputStream or Writer)
	 * @throws IOException if the template file could not be retrieved
	 * @throws TemplateException if thrown by FreeMarker
	 * @see freemarker.template.Template#process(Object, java.io.Writer)
	 */
	protected void processTemplate(Template template, SimpleHash model, HttpServletResponse response)
			throws IOException, TemplateException {

		template.process(model, response.getWriter());
	}


	/**
	 * Extension of FreeMarker {@link SimpleHash}, adding a fallback to request attributes.
	 * Similar to the formerly used {@link freemarker.ext.servlet.AllHttpScopesHashModel},
	 * just limited to common request attribute exposure.
	 */
	@SuppressWarnings("serial")
	private static class RequestHashModel extends SimpleHash {

		private final HttpServletRequest request;

		public RequestHashModel(ObjectWrapper wrapper, HttpServletRequest request) {
			super(wrapper);
			this.request = request;
		}

		@Override
		public TemplateModel get(String key) throws TemplateModelException {
			TemplateModel model = super.get(key);
			if (model != null) {
				return model;
			}
			Object obj = this.request.getAttribute(key);
			if (obj != null) {
				return wrap(obj);
			}
			return wrap(null);
		}
	}

}

package com.au.sortfeature.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.au.sortfeature.Activator;
import com.au.sortfeature.preferences.PreferenceConstants;

public class SortFeatureHandler extends AbstractHandler implements IHandler {

	private final Pattern FEATURE_PATTERN = Pattern.compile("^.*<feature[^>]+>$", Pattern.DOTALL | Pattern.MULTILINE);
	private final Pattern INSTALL_HANDLER_PATTERN = Pattern.compile("^\\s*<install-handler[^>]+>$", Pattern.DOTALL
			| Pattern.MULTILINE);
	private final Pattern DESCRIPTION_PATTERN = Pattern.compile("^\\s*<description.*?</description>", Pattern.DOTALL
			| Pattern.MULTILINE);
	private final Pattern COPYRIGHT_PATTERN = Pattern.compile("^\\s*<copyright.*?</copyright>", Pattern.DOTALL
			| Pattern.MULTILINE);
	private final Pattern LICENSE_PATTERN = Pattern.compile("^\\s*<license.*?</license>", Pattern.DOTALL
			| Pattern.MULTILINE);
	private final Pattern URL_PATTERN = Pattern.compile("^\\s*<url.*?</url>", Pattern.DOTALL | Pattern.MULTILINE);
	private final Pattern INCLUDES_PATTERN = Pattern.compile("^\\s*<includes[^>]+>$", Pattern.DOTALL | Pattern.MULTILINE);
	private final Pattern REQUIRES_PATTERN = Pattern.compile("^\\s*<requires>.*</requires>", Pattern.DOTALL
			| Pattern.MULTILINE);
	private final Pattern IMPORT_PLUGIN_PATTERN = Pattern.compile("^\\s*<import plugin[^>]+?>$", Pattern.DOTALL
			| Pattern.MULTILINE);
	private final Pattern IMPORT_FEATURE_PATTERN = Pattern.compile("^\\s*<import feature[^>]+?>$", Pattern.DOTALL
			| Pattern.MULTILINE);
	private final Pattern PLUGIN_PATTERN = Pattern.compile("^\\s*<plugin[^>]+?>$", Pattern.DOTALL | Pattern.MULTILINE);
	private final Pattern DATA_PATTERN = Pattern.compile("^\\s*<data[^>]+?>$", Pattern.DOTALL | Pattern.MULTILINE);

	private List<IFile> featureXmls;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		if (currentSelection == null) {
			return null;
		}
		if (currentSelection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) currentSelection;
			List<?> selections = structuredSelection.toList();
			featureXmls = new ArrayList<IFile>();
			for (Object obj : selections) {
				if (obj instanceof IResource) {
					IResource resource = (IResource) obj;
					IProject project = resource.getProject();
					IProjectNature projectNature;
					try {
						projectNature = project.getNature("org.eclipse.pde.FeatureNature");
						if (projectNature != null) {
							IFile featureXml = project.getFile("feature.xml");
							featureXmls.add(featureXml);
						}
					} catch (CoreException e) {
						// project does not exist or is closed or Feature nature
						// extension was not found. Carry on.
					}
				}
			}
			if (featureXmls.isEmpty()) {
				// No features have been selected to sort.
				return null;
			} else {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				ISchedulingRule schedulingRule = MultiRule.combine(featureXmls.toArray(new IFile[] {}));
				try {
					workspace.run(workspaceRunnable, schedulingRule, IWorkspace.AVOID_UPDATE, null);
				} catch (CoreException e) {
					e.printStackTrace();
					return null;
				}
			}
		}
		return null;
	}

	private IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {

		@Override
		public void run(IProgressMonitor monitor) throws CoreException {
			for (IFile featureXml : featureXmls) {
				sortFeature(featureXml);
			}

		}

		private void sortFeature(IFile featureXml) {
			String content = getStringFromIFile(featureXml);
			if (content == null) {
				System.out.println("Unable to read feature.xml content.");
				return;
			}
			System.out.println(content);
			String output = sortFeatureXmlString(content);
			System.out.println(output);
		}

		private String sortFeatureXmlString(String content) {
			IPreferenceStore store = Activator.getDefault().getPreferenceStore();
			StringBuilder sb = new StringBuilder();
			Matcher m = FEATURE_PATTERN.matcher(content);
			m.find();
			String featureString = m.group();
			sb.append(featureString).append(System.lineSeparator());
			m = INSTALL_HANDLER_PATTERN.matcher(content);
			if (m.find()) {
				String installHandlerString = m.group();
				sb.append(installHandlerString).append(System.lineSeparator());
			}
			m = DESCRIPTION_PATTERN.matcher(content);
			if (m.find()) {
				String descriptionString = m.group();
				sb.append(descriptionString).append(System.lineSeparator());
			}
			m = COPYRIGHT_PATTERN.matcher(content);
			if (m.find()) {
				String copyrightString = m.group();
				sb.append(copyrightString).append(System.lineSeparator());
			}
			m = LICENSE_PATTERN.matcher(content);
			if (m.find()) {
				String licenceString = m.group();
				sb.append(licenceString).append(System.lineSeparator());
			}
			m = URL_PATTERN.matcher(content);
			if (m.find()) {
				String urlString = m.group();
				sb.append(urlString).append(System.lineSeparator());
			}
			m = INCLUDES_PATTERN.matcher(content);
			List<String> includesStrings = new ArrayList<String>();
			while (m.find()) {
				includesStrings.add(m.group());
			}
			if (store.getBoolean(PreferenceConstants.SORT_INCLUDE_FEATURES)) {
				Collections.sort(includesStrings);
			}
			for (String string : includesStrings) {
				sb.append(string).append(System.lineSeparator());
			}
			m = REQUIRES_PATTERN.matcher(content);
			if (m.find()) {
				String requiresString = m.group();
				m = IMPORT_FEATURE_PATTERN.matcher(requiresString);
				List<String> importFeatureStrings = new ArrayList<String>();
				while (m.find()) {
					importFeatureStrings.add(m.group());
				}
				if (!importFeatureStrings.isEmpty() || !store.getBoolean(PreferenceConstants.DELETE_IMPORT_PLUGINS)) {
					sb.append(System.lineSeparator()).append("   <requires>").append(System.lineSeparator());
					if (!importFeatureStrings.isEmpty()) {
						Collections.sort(importFeatureStrings);
						for (String string : importFeatureStrings) {
							sb.append(string).append(System.lineSeparator());
						}
					}
					if (store.getBoolean(PreferenceConstants.DELETE_IMPORT_PLUGINS)) {
						m = IMPORT_PLUGIN_PATTERN.matcher(requiresString);
						List<String> importPluginStrings = new ArrayList<String>();
						while (m.find()) {
							importPluginStrings.add(m.group());
						}
						Collections.sort(importPluginStrings);
						for (String string : importPluginStrings) {
							sb.append(string).append(System.lineSeparator());
						}
					}
					sb.append("   </requires>").append(System.lineSeparator());
				}
			}
			m = PLUGIN_PATTERN.matcher(content);
			List<String> pluginStrings = new ArrayList<String>();
			while (m.find()) {
				pluginStrings.add(m.group());
			}
			Collections.sort(pluginStrings);
			for (String string : pluginStrings) {
				sb.append(string).append(System.lineSeparator());
			}
			m = DATA_PATTERN.matcher(content);
			List<String> dataStrings = new ArrayList<String>();
			while (m.find()) {
				dataStrings.add(m.group());
			}
			Collections.sort(dataStrings);
			for (String string : dataStrings) {
				sb.append(string).append(System.lineSeparator());
			}
			sb.append(System.lineSeparator()).append("</feature>").append(System.lineSeparator());
			return sb.toString();
		}

		private String getStringFromIFile(IFile featureXml) {
			if (!featureXml.exists())
				return null;
			InputStream inputStream = null;
			BufferedReader br = null;
			StringBuilder sb = new StringBuilder();
			try {
				inputStream = featureXml.getContents();
				br = new BufferedReader(new InputStreamReader(inputStream));
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line).append(System.lineSeparator());
				}
			} catch (CoreException e) {
				System.err.println("Cannot read file [ " + featureXml.getFullPath() + " ]");
				return null;
			} catch (IOException e) {
				System.err.println("Error reading file [ " + featureXml.getFullPath() + " ]");
				e.printStackTrace();
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						// not to worry
					}
				}
			}
			return sb.toString();
		}
	};
}

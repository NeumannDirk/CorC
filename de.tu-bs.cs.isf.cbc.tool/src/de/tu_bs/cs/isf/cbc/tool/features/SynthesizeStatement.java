package de.tu_bs.cs.isf.cbc.tool.features;

import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;

import de.tu_bs.cs.isf.cbc.cbcmodel.AbstractStatement;
import de.tu_bs.cs.isf.cbc.cbcmodel.CbCFormula;
import de.tu_bs.cs.isf.cbc.cbcmodel.GlobalConditions;
import de.tu_bs.cs.isf.cbc.cbcmodel.JavaVariable;
import de.tu_bs.cs.isf.cbc.cbcmodel.JavaVariables;
import de.tu_bs.cs.isf.cbc.cbcmodel.Renaming;
import de.tu_bs.cs.isf.cbc.cbcmodel.ReturnStatement;
import de.tu_bs.cs.isf.cbc.cbcmodel.SkipStatement;
import de.tu_bs.cs.isf.cbc.cbcmodel.impl.AbstractStatementImpl;
import de.tu_bs.cs.isf.cbc.cbcmodel.impl.CbCFormulaImpl;
import de.tu_bs.cs.isf.cbc.tool.helper.GenerateCodeForVariationalVerification;
import de.tu_bs.cs.isf.cbc.util.CompareMethodBodies;
import de.tu_bs.cs.isf.cbc.util.Console;
import de.tu_bs.cs.isf.cbc.util.DiagramPartsExtractor;
import de.tu_bs.cs.isf.cbc.util.FeatureUtil;
import de.tu_bs.cs.isf.cbc.util.FileHandler;
import de.tu_bs.cs.isf.cbc.util.FileUtil;
import de.tu_bs.cs.isf.cbc.util.GetDiagramUtil;
import de.tu_bs.cs.isf.cbc.util.ProveWithKey;
import de.tu_bs.cs.isf.cbc.util.VerifyFeatures;
import de.tu_bs.cs.isf.cbc.util.statistics.StatDataCollector;
import records.SynthesisInformation;

/**
 * Class that changes the abstract value of algorithms
 * 
 * @author Tobias
 *
 */
public class SynthesizeStatement extends MyAbstractAsynchronousCustomFeature {

	/**
	 * Constructor of the class
	 * 
	 * @param fp The FeatureProvider
	 */
	public SynthesizeStatement(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public String getName() {
		return "Synthesize a statement";
	}

	@Override
	public String getDescription() {
		return "Synthesize a statement.";
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		boolean ret = false;
		PictogramElement[] pes = context.getPictogramElements();
		if (pes != null && pes.length == 1) {
			Object bo = getBusinessObjectForPictogramElement(pes[0]);
			if (bo != null && (bo.getClass().equals(AbstractStatementImpl.class) || bo instanceof SkipStatement
					|| bo instanceof ReturnStatement)) {
				AbstractStatement statement = (AbstractStatement) bo;
				if (statement.getRefinement() == null) {
					ret = true;
				}
			}
		}
		return ret;
	}

	@Override
	public void execute(ICustomContext context, IProgressMonitor monitor) {
		verifyStatement(context, monitor, false);
	}

	private SynthesisInformation askUserForMutability(AbstractStatement as) {
		// collect data
		EObject container = as.eContainer();
		while (container.eContainer() != null) {
			container = container.eContainer();
		}
		String[] varNames = null;
		if (container instanceof CbCFormulaImpl) {
			CbCFormulaImpl cbcFormula = (CbCFormulaImpl) container;
			Resource eStorage = cbcFormula.eResource();
			for (EObject eo : eStorage.getContents()) {
				if (eo instanceof JavaVariables) {
					EList<JavaVariable> javaVariables = ((JavaVariables) eo).getVariables();
					varNames = new String[javaVariables.size()];
					for (int index = 0; index < javaVariables.size(); index++) {
						JavaVariable javaVar = javaVariables.get(index);
						varNames[index] = javaVar.getName().split(" ")[1];
					}
				}
			}
		}

		// single UI elements
		JLabel mutLabel = new JLabel("Select the mutable variables for this statement:");
		ArrayList<JCheckBox> checkBoxes = new ArrayList<>();
		if (varNames != null) {
			for (String element : varNames) {
				JCheckBox newBox = new JCheckBox(element);
				newBox.setSelected(true);
				checkBoxes.add(newBox);
			}
		}
		JLabel loopLabel = new JLabel("Select if this statement has to function as a loop variant update:");
		JCheckBox additionalCheckBox = new JCheckBox("This statement is a loop variant update");
		// additionalCheckBox.setSelected(false);
		additionalCheckBox.setSelected(true);

		// build dialog window
		JPanel panel = new JPanel(new GridLayout(checkBoxes.size() + 3, 1));
		panel.add(mutLabel);
		for (JCheckBox checkBox : checkBoxes) {
			panel.add(checkBox);
		}
		panel.add(loopLabel);
		panel.add(additionalCheckBox);

		// show dialog
		ArrayList<String> mutableVarNames = new ArrayList<String>();
		int result = JOptionPane.showConfirmDialog(null, panel, "Select Options", JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			for (int checkBoxIndex = 0; checkBoxIndex < checkBoxes.size(); checkBoxIndex++) {
				JCheckBox checkBox = checkBoxes.get(checkBoxIndex);
				if (checkBox.isSelected()) {
					mutableVarNames.add(varNames[checkBoxIndex]);
				}
			}
		}

		return new SynthesisInformation(mutableVarNames.toArray(new String[0]),
				additionalCheckBox.isSelected());
	}

	private String concatenateStrings(String[] strings, String separator, String prefix, String suffix) {
		StringBuilder result = new StringBuilder();
		result.append(prefix);
		for (int i = 0; i < strings.length; i++) {
			if (i > 0) {
				result.append(separator);
			}
			result.append(strings[i]);
		}
		result.append(suffix);
		return result.toString();
	}

	void verifyStatement(ICustomContext context, IProgressMonitor monitor, boolean inlining) {
		long startTime = System.nanoTime();
		// JOptionPane.showConfirmDialog(null, "Hello");
		Console.clear();
		monitor.beginTask("Verify statement", IProgressMonitor.UNKNOWN);
		PictogramElement[] pes = context.getPictogramElements();
		if (pes != null && pes.length == 1) {
			Object bo = getBusinessObjectForPictogramElement(pes[0]);
			if (bo instanceof AbstractStatement) {
				boolean isReturnStatement = bo instanceof ReturnStatement;
				AbstractStatement statement = (AbstractStatement) bo;
				SynthesisInformation userFeedback = askUserForMutability(statement);

				String additionalHeader = "//statementid:{" + statement.getId() + "};\n"
						+ this.concatenateStrings(userFeedback.getMutableVariables(), ",", "//mutable:{", "};\n")
						+ "//isLoopUpdate:{" + (userFeedback.getIsLoopVariantUpdate() ? "true" : "false") + "};\n";

				File keyFile = prepareKeYFile(statement, getDiagram(), monitor, additionalHeader,
						userFeedback.getIsLoopVariantUpdate());

				statement.setName("???");
				updatePictogramElement(((Shape) pes[0]).getContainer());
			}
		}
		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1000000;
		Console.println("\nVerification done.");
		Console.println("Time needed: " + duration + "ms");
		monitor.done();
	}

	private File prepareKeYFile(AbstractStatement statement, Diagram diagram, IProgressMonitor monitor,
			String additionalHeader, boolean isLoopVariantUpdate) {
		StatDataCollector.checkForId(statement);
		Console.println("Starting verification...\n");
		URI uri = getDiagram().eResource().getURI();
		String platformUri = uri.toPlatformString(true);
		String callingClass = FeatureUtil.getInstance().getCallingClass(uri);
		ProveWithKey prove = new ProveWithKey(statement, getDiagram(), monitor, new FileUtil(platformUri), "", "");
		return prove.createProveStatementWithKey(null, null, null, true, "", "", false, callingClass, additionalHeader,
				isLoopVariantUpdate);
	}
}
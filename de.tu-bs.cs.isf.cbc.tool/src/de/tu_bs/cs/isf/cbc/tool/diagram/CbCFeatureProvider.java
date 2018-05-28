package de.tu_bs.cs.isf.cbc.tool.diagram;

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IDirectEditingFeature;
import org.eclipse.graphiti.features.IReconnectionFeature;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.features.context.IDirectEditingContext;
import org.eclipse.graphiti.features.context.IReconnectionContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.pattern.DefaultFeatureProviderWithPatterns;

import de.tu_bs.cs.isf.cbc.cbcmodel.Variant;
import de.tu_bs.cs.isf.cbc.tool.features.AddPseudoCodeToMethodFeature;
import de.tu_bs.cs.isf.cbc.tool.features.ChangeNameOfFormulaFeature;
import de.tu_bs.cs.isf.cbc.tool.features.CreateExtraSelectionFeature;
import de.tu_bs.cs.isf.cbc.tool.features.DeleteConnectionFeature;
import de.tu_bs.cs.isf.cbc.tool.features.DrillDownFeature;
import de.tu_bs.cs.isf.cbc.tool.features.DrillUpFeature;
import de.tu_bs.cs.isf.cbc.tool.features.EditCommentFeature;
import de.tu_bs.cs.isf.cbc.tool.features.EditVariantFeature;
import de.tu_bs.cs.isf.cbc.tool.features.GenerateTextualRepresentation;
import de.tu_bs.cs.isf.cbc.tool.features.PrintFormulaFeature;
import de.tu_bs.cs.isf.cbc.tool.features.ReconnectionFeature;
import de.tu_bs.cs.isf.cbc.tool.features.VerifyMethodStatementAndSubFormula;
import de.tu_bs.cs.isf.cbc.tool.features.VerifyPostRepetitionStatement;
import de.tu_bs.cs.isf.cbc.tool.features.VerifyPreRepetitionStatement;
import de.tu_bs.cs.isf.cbc.tool.features.VerifyPreSelectionStatement;
import de.tu_bs.cs.isf.cbc.tool.features.VerifyStatement;
import de.tu_bs.cs.isf.cbc.tool.features.VerifyVariant;
import de.tu_bs.cs.isf.cbc.tool.features.VerifyVariant2;
import de.tu_bs.cs.isf.cbc.tool.features.VerifyVariant3;
import de.tu_bs.cs.isf.cbc.tool.patterns.Composition3Pattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.CompositionPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.ConditionPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.ConnectionPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.FormulaPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.GlobalConditionsPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.MethodPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.MethodStatementPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.RenamePattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.RenamingPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.RepetitionPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.SelectionPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.SkipStatementPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.SmallRepetitionPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.StatementPattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.VariablePattern;
import de.tu_bs.cs.isf.cbc.tool.patterns.VariablesPattern;

public class CbCFeatureProvider extends DefaultFeatureProviderWithPatterns {

	public CbCFeatureProvider(IDiagramTypeProvider dtp) {
        super(dtp);
        addPattern(new FormulaPattern());
        addPattern(new CompositionPattern());
        addPattern(new Composition3Pattern());
        addPattern(new SelectionPattern());
        addPattern(new RepetitionPattern());
        addPattern(new SmallRepetitionPattern());
        addPattern(new SkipStatementPattern());
        addPattern(new MethodStatementPattern());
        addPattern(new StatementPattern());
        addPattern(new MethodPattern());
        addPattern(new GlobalConditionsPattern());
        addPattern(new ConditionPattern());
        addPattern(new VariablesPattern());
        addPattern(new VariablePattern());
        addPattern(new RenamingPattern());
        addPattern(new RenamePattern());
        addConnectionPattern(new ConnectionPattern());
    }
	
	@Override
	public ICreateFeature[] getCreateFeatures() {
		ICreateFeature[] oldArray = super.getCreateFeatures();
		ICreateFeature[] array = new ICreateFeature[oldArray.length + 1];
		for (int i = 0; i < oldArray.length; i++) {
			array[i] = oldArray[i];
		}
		array[array.length - 1] = new CreateExtraSelectionFeature(this);
	   return array;
	}
	
	@Override
	public IDirectEditingFeature getDirectEditingFeature(IDirectEditingContext context) {
	    PictogramElement pe = context.getPictogramElement();
	    Object bo = getBusinessObjectForPictogramElement(pe);
	    if (bo instanceof Variant) {
	        return new EditVariantFeature(this);
	    }
	    return super.getDirectEditingFeature(context);
	}
	
	@Override
	public IReconnectionFeature getReconnectionFeature(IReconnectionContext context) {
		return new ReconnectionFeature(this);
	}
	
//	@Override
//	public IUpdateFeature getUpdateFeature(IUpdateContext context) {
//		PictogramElement pe = context.getPictogramElement();
//	    Object bo = getBusinessObjectForPictogramElement(pe);
//	    if (bo instanceof JavaVariable) {
//	        return new UpdateVariableFeature(this);
//	    }
//	   return super.getUpdateFeature(context);
//	} 
	
	@Override
	public IDeleteFeature getDeleteFeature(IDeleteContext context) {
		if (context.getPictogramElement() instanceof Connection) {
			return new DeleteConnectionFeature(this);
		}
		return super.getDeleteFeature(context);
	}
	
	@Override
	public ICustomFeature[] getCustomFeatures(ICustomContext context) {
	    return new ICustomFeature[] { 
	    		new AddPseudoCodeToMethodFeature(this), 
	    		new PrintFormulaFeature(this),
	    		new GenerateTextualRepresentation(this),
	    		new VerifyStatement(this),
	    		new VerifyPreRepetitionStatement(this),
	    		new VerifyPostRepetitionStatement(this),
	    		new VerifyPreSelectionStatement(this),
	    		new VerifyVariant(this),
	    		new VerifyVariant2(this),
	    		new VerifyVariant3(this),
	    		new EditCommentFeature(this),
	    		new DrillDownFeature(this),
	    		new DrillUpFeature(this),
	    		new ChangeNameOfFormulaFeature(this),
	    		new VerifyMethodStatementAndSubFormula(this)};
	}
}
package net.sf.jxls.formula;

import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import net.sf.jxls.tag.Point;
import net.sf.jxls.transformation.BlockTransformation;
import net.sf.jxls.transformation.DuplicateTransformation;
import net.sf.jxls.transformer.Workbook;

import java.util.*;

/**
 * @author Leonid Vysochyn
 */
public class FormulaControllerImpl implements FormulaController {
    protected final Log log = LogFactory.getLog(getClass());

    protected Map sheetFormulasMap;


    Workbook workbook;

    public FormulaControllerImpl(Workbook workbook) {
        this.workbook = workbook;
        sheetFormulasMap = workbook.createFormulaSheetMap();
    }

    public void updateWorkbookFormulas(BlockTransformation transformation){
        Set sheetNames = sheetFormulasMap.keySet();
        Formula formula, newFormula;
        Set cellRefs, newCellRefs;
        CellRef cellRef, newCellRef;
        List resultCells;
        String newCell;
        Point p, point, newPoint;
        Set cellRefsToRemove = new HashSet();
        Set formulasToRemove = new HashSet();
        for (Iterator iterator = sheetNames.iterator(); iterator.hasNext();) {
            String sheetName =  (String) iterator.next();
            List formulas = (List) sheetFormulasMap.get( sheetName );
            formulasToRemove.clear();
            for (int i = 0, size = formulas.size(); i < size; i++) {
                formula = (Formula) formulas.get(i);
                cellRefs = formula.getCellRefs();
                cellRefsToRemove.clear();
                for (Iterator iter = cellRefs.iterator(); iter.hasNext();) {
                    cellRef = (CellRef) iter.next();
                    if( !(transformation instanceof DuplicateTransformation && transformation.getBlock().contains(cellRef) &&
                            transformation.getBlock().contains( formula.getRowNum().intValue(), formula.getCellNum().intValue())) ){
                        resultCells = transformation.transformCell( sheetName, cellRef );
                        if( resultCells != null ){
                            if( resultCells.size() == 1 ){
                                newCell = (String) resultCells.get(0);
                                cellRef.update( newCell );
                            }else if( resultCells.size() > 1 ){
                                cellRef.update( resultCells );
                            }
                        }else {
                            cellRefsToRemove.add( cellRef );
                        }
                    }
                }
//                cellRefs.removeAll( cellRefsToRemove );
                if( !cellRefsToRemove.isEmpty() ){
                    formula.removeCellRefs( cellRefsToRemove );
                }
                formula.updateReplacedRefCellsCollection();
                if( formula.getSheet().getSheetName().equals( transformation.getBlock().getSheet().getSheetName() ) ){
                    p = new Point( formula.getRowNum().intValue(), formula.getCellNum().shortValue() );
                    List points = transformation.transformCell( p );
                    if( points!=null && !points.isEmpty()){
                        if(points.size() == 1){
                            newPoint = (Point) points.get(0);
                            formula.setRowNum( new Integer( newPoint.getRow() ));
                            formula.setCellNum( new Integer( newPoint.getCol() ));
                        }else{
                            List sheetFormulas = (List) sheetFormulasMap.get( formula.getSheet().getSheetName() );
                            for (int j = 1, num = points.size(); j < num; j++) {
                                point = (Point) points.get(j);
                                newFormula = new Formula( formula );
                                newFormula.setRowNum( new Integer(point.getRow()) );
                                newFormula.setCellNum( new Integer(point.getCol() ) );
                                newCellRefs = newFormula.getCellRefs();
                                for (Iterator iterator1 = newCellRefs.iterator(); iterator1.hasNext();) {
                                    newCellRef =  (CellRef) iterator1.next();
                                    if( transformation.getBlock().contains( newCellRef ) && transformation.getBlock().contains( p ) ){
                                        newCellRef.update(transformation.getDuplicatedCellRef( sheetName, newCellRef.toString(), j));
                                    }
                                }
                                sheetFormulas.add( newFormula );
                            }
                        }
                    }else{
                        if( points == null ){
                            // remove formula
                            formulasToRemove.add( formula );
                        }
                    }
                }
            }
            formulas.removeAll( formulasToRemove );
        }
//        if( log.isDebugEnabled() ){
//            writeFormulas( new CommonFormulaResolver() );
//            net.sf.jxls.util.Util.writeToFile("test.xls", transformation.getBlock().getSheet().getHssfWorkbook());
//        }
    }

    public Map getSheetFormulasMap() {
        return sheetFormulasMap;
    }

    public void writeFormulas(FormulaResolver formulaResolver) {
        Set sheetNames = sheetFormulasMap.keySet();
        for (Iterator iterator = sheetNames.iterator(); iterator.hasNext();) {
            String sheetName =  (String) iterator.next();
            List formulas = (List) sheetFormulasMap.get( sheetName );
            for (int i = 0; i < formulas.size(); i++) {
                Formula formula = (Formula) formulas.get(i);
                String formulaString = formulaResolver.resolve( formula, null);
                HSSFRow hssfRow = formula.getSheet().getHssfSheet().getRow(formula.getRowNum().intValue());
                HSSFCell hssfCell = hssfRow.getCell(formula.getCellNum().shortValue());
                if (formulaString != null) {
                    if( hssfCell == null ){
                        hssfCell = hssfRow.createCell( formula.getCellNum().shortValue() );
                    }
                    try {
                        hssfCell.setCellFormula(formulaString);
                    } catch (RuntimeException e) {
                        log.error("Can't set formula: " + formulaString, e);
                        throw new RuntimeException("Can't set formula: " + formulaString, e );
                    }
                }
            }
        }
    }

 }

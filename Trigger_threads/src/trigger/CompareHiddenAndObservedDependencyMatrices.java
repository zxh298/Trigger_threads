package trigger;

import java.util.ArrayList;

public class CompareHiddenAndObservedDependencyMatrices {

	/**
	 * Compare the dependency matrices of hidden models (including a single hidden variable as 
	 * a common cause) and observed models (without any hidden variables):
	 * 
	 * 1) If they are totally same, it means that all their dependencies in terms of the same set
	 *    of condition variable are same (involving the hidden variable does not make any change).
	 * 
	 * 2) If they are different, it means that involving the hidden variable has provided different
	 *    dependency relationships among the remaining observed variables in the model, and thus we
	 *    should keep this hidden variable.
	 * 
	 * */

	private ArrayList<int[][]> hiddenDependencyMatrices = new ArrayList<int[][]>();
	private ArrayList<int[]> allObservedDAGsDependencyMatricesValues = new ArrayList<int[]>();
	private int observedVarNum;
    // If there exists observed models with the same dependencies as the current hidden model,
	// store their index:
	//private ArrayList<Integer> indexOfObservedModelsHaveSameDependencies = new ArrayList<Integer>();

//	public CompareHiddenAndObservedDependencyMatrices(ArrayList<int[][]> hiddenDependencyMatrices, 
//			ArrayList<ArrayList<int[][]>> allObservedDAGsDependencyMatrices, int observedVarNum)
//	{
//		this.hiddenDependencyMatrices = (ArrayList<int[][]>) hiddenDependencyMatrices;
//		this.allObservedDAGsDependencyMatrices = (ArrayList<ArrayList<int[][]>>) allObservedDAGsDependencyMatrices;
//		this.observedVarNum = observedVarNum;
//	}

	public CompareHiddenAndObservedDependencyMatrices(ArrayList<int[][]> hiddenDependencyMatrices, 
			ArrayList<int[]> allObservedDAGsDependencyMatricesValues, int observedVarNum)
	{
		this.hiddenDependencyMatrices =  hiddenDependencyMatrices;
		this.allObservedDAGsDependencyMatricesValues = allObservedDAGsDependencyMatricesValues;
		this.observedVarNum = observedVarNum;
	}
	
	private ArrayList<int[][]> reduceMetricesDimension()
	{
		/**
		 *  Because we do not care about the dependency relationships between the hidden variable 
		 *  and other observed variables, so we should remove the dimension of hidden variable in
		 *  the dependency matrices in order to compare with observed dependency matrices.
		 * 
		 *  For example:
		 *  
		 *    H 1 2 3                  1 2 3 
		 *   ---------                -------
		 * H| 0 1 0 1     ------->  1| 0 1 0 
		 * 1| 1 0 1 0               2| 1 0 1
		 * 2| 0 1 0 1               3| 0 1 0
		 * 3| 1 0 1 0
		 * 
		 * */

		ArrayList<int[][]> resultList = new ArrayList<int[][]>();

		for(int[][] currentMatrix : hiddenDependencyMatrices)
		{
			int[][] temp = new int[observedVarNum][observedVarNum];
			for(int n = 0; n < observedVarNum; n++)
			{
				for(int m = 0; m < observedVarNum; m++)
					temp[n][m] = currentMatrix[n+1][m+1];
			}
			resultList.add(temp);
		}

		return resultList;
	}

	public boolean hiddenAndObeservedDependencySame()
	{

		ArrayList<int[][]> reducedHiddenDependencyMatrices = reduceMetricesDimension();
		ComputeMatrixValue cm = new ComputeMatrixValue(reducedHiddenDependencyMatrices);
		int[] reducedHiddenDependencyMatrixValues = cm.getMatrixValues();
		/**
		 * numberOfPbservedDAGsHaveDifferentDependency: 
		 * The total number of observed DAGs that have the same dependency 
		 * structure as the current hidden DAG.
		 * */
		int differentNum = 0;
		
		for(int num = 0; num < allObservedDAGsDependencyMatricesValues.size(); num++)
		{			
			//Get the dependency matrices list of current num(th) observed DAG:
			int[] currentObservedDependencyMatrixValue = allObservedDAGsDependencyMatricesValues.get(num);

			for(int i=0; i<reducedHiddenDependencyMatrixValues.length; i++)
			{
			     if(reducedHiddenDependencyMatrixValues[i] != currentObservedDependencyMatrixValue[i])
			     {
			    	 differentNum++;
			    	 break;
			     }
			}
		
		}
		
		if(differentNum == allObservedDAGsDependencyMatricesValues.size())
			return false;
        
		return true;
	}

}

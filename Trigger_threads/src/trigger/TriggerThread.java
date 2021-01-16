package trigger;

import java.util.ArrayList;

/**
 * This class aims to generate all unique DAGs using multi-threads, thus makes
 * the searching process more efficient.
 * 
 * Dec 5, 2017 Xuhui Zhang
 * 
 */

public class TriggerThread implements Runnable
{

	private Thread thread;
	private ArrayList<int[]> subInput;
	private ArrayList<int[][]> subInput2;
	private ArrayList<int[][]> subInput3;
	private int varNum;
	private String threadName;
	private ArrayList<int[][]> subResults;
	private ArrayList<int[]> subResults2;
	private ArrayList<int[][]> subResults3;
	private ArrayList<int[]> allObservedDAGsDependencyMaticesValues;
	private ArrayList<int[]> conditionVarList;
	private ArrayList<int[]> remainingVarList;
	private int flag;
	private boolean allow_disconnected_nodes;

	public TriggerThread(ArrayList<int[]> subInput, int varNum, String name, boolean allow_disconnected_nodes) {
		this.subInput = subInput;
		this.varNum = varNum;
		this.threadName = name;
		this.flag = 0;
		this.allow_disconnected_nodes = allow_disconnected_nodes;
	}

	public TriggerThread(ArrayList<int[][]> subInput2, String name, 
			ArrayList<int[]> conditionVarList, ArrayList<int[]> remainingVarList) {
		this.subInput2 = subInput2;
		this.varNum = subInput2.get(0).length;
		this.threadName = name;
		this.conditionVarList = conditionVarList;
		this.remainingVarList = remainingVarList;
		this.flag = 1;
	}

	public TriggerThread(ArrayList<int[][]> subInput3, String name,
			ArrayList<int[]> allObservedDAGsDependencyMaticesValues, 
			ArrayList<int[]> conditionVarList, ArrayList<int[]> remainingVarList) {
		this.subInput3 = subInput3;
		this.varNum = subInput3.get(0).length;
		this.threadName = name;
		this.allObservedDAGsDependencyMaticesValues = allObservedDAGsDependencyMaticesValues;
		this.conditionVarList = conditionVarList;
		this.remainingVarList = remainingVarList;
		subResults3 = new ArrayList<int[][]>();
		this.flag = 2;
	}

	public void run()
	{
		if (threadName != null)
			System.out.println("Running " + threadName + " to generate all DAGs...");

		try
		{
			if (flag == 0)
				subResults = GenerateAllDAGs.constructFullMatrixList(subInput, varNum, allow_disconnected_nodes);
			if (flag == 1)
			{
				subResults2 = getObservedDAGDependencyMatrices();
			}

			if (flag == 2)
			{
				getHiddenModels();
			}

			// Let the thread sleep for a while.
			Thread.sleep(50);

		} catch (InterruptedException e)
		{
			if (threadName != null)
				System.out.println(threadName + " interrupted!");
			else
				System.out.println("Thread interrupted!");
		}

	}

	private ArrayList<int[]> getObservedDAGDependencyMatrices()
	{

		ArrayList<int[]> result = new ArrayList<int[]>();

		for (int[][] currentObservedDAG : subInput2)
		{
			GetFullLablesCombinationList gc = new GetFullLablesCombinationList(currentObservedDAG, false);

			ArrayList<int[][]> allObservedLabelsCombinationDAGList = gc.getLablesCombinationDAGList();

			for (int[][] currentDAG : allObservedLabelsCombinationDAGList)
			{
				GenerateAllDependencies gd = new GenerateAllDependencies(currentDAG, varNum, conditionVarList, remainingVarList, false);
				ArrayList<int[][]> dependencyMatricesOfCurrentObservedDAG = gd.getResult();

				// add in the matrix into the dictionary:
				ComputeMatrixValue cm = new ComputeMatrixValue(dependencyMatricesOfCurrentObservedDAG);
				int[] dependencyMatricesValuesOfCurrentObservedDAG = cm.getMatrixValues();
				result.add(dependencyMatricesValuesOfCurrentObservedDAG);
			}
			
			int index = subInput2.indexOf(currentObservedDAG);
			
			double percentage = (double)index/subInput2.size();
			
			String percentage_string = String.format("%.0f", percentage*100);
			
			if(percentage%5 < 1.0 && !percentage_string.equals("0"))
				System.out.println(threadName + " Generate DAG dependencies finishing " + percentage_string + "%...");
		}

		return result;
	}

	// get candidate hidden models, may found by multiple threads
	private void getHiddenModels()
	{
		for (int[][] currentObservedDAG : subInput3)
		{
			GenerateHiddenVarDAGList gv = new GenerateHiddenVarDAGList(currentObservedDAG, varNum);

			ArrayList<int[][]> hiddenModelOfCurrenObservedDAG = gv.makeHiddenMatrix();

			// By default, we believe the dependency relationships among current
			// observed DAG and corresponding hidden DAG are same:
			boolean isSame = true;
			int falseNum = 0;
			// For each hidden DAG, compare its dependency matrices with the
			// current observed DAG's dependency matrices:

			for (int[][] currentHiddenModel : hiddenModelOfCurrenObservedDAG)
			{
				// to store the number of label combination structures which
				// have different dependency structures as the current candidate hidden model:
				falseNum = 0;

				// Get all label combination DAGs of current hidden DAG:
				GetFullLablesCombinationList gcHidden = new GetFullLablesCombinationList(currentHiddenModel, true);
				ArrayList<int[][]> allHiddenLabelsCombinationDAGList = gcHidden.getLablesCombinationDAGList();

				for (int[][] currentHiddenDAG : allHiddenLabelsCombinationDAGList)
				{
					isSame = true;
					GenerateAllDependencies ghd = new GenerateAllDependencies(currentHiddenDAG, varNum + 1,  conditionVarList, remainingVarList, true);
					ArrayList<int[][]> dependencyMatricesOfCurrentHiddenDAG = ghd.getResult();
					CompareHiddenAndObservedDependencyMatrices compare = new CompareHiddenAndObservedDependencyMatrices(
							dependencyMatricesOfCurrentHiddenDAG, allObservedDAGsDependencyMaticesValues, varNum);

					isSame = compare.hiddenAndObeservedDependencySame();

					if (isSame == false)
						falseNum++;
				}

				// ****************************************************
				// If there is even one dependency values in the two matrices
				// are different, then the current hidden DAG provide a different dependency
				// structure in terms of D-Separation:
				if (falseNum == allHiddenLabelsCombinationDAGList.size())
				{
					subResults3.add(currentHiddenModel);
				}

			}
			
            int index = subInput3.indexOf(currentObservedDAG);
			
			double percentage = (double)index/subInput3.size();
			
			percentage = Math.round(percentage * 10000);
			
//			String percentage_string = String.format("##.00", percentage*100);
			
			if(percentage%2 < 1.0 && percentage/10000 > 0)
				System.out.println(threadName + " Searching triggers finishing " + percentage/100 + "%...");

		}
	}
	
	public void start()
	{
		if (threadName != null)
			System.out.println("Starting " + threadName + "...");

		if (thread == null)
		{
			thread = new Thread(this, threadName);
			thread.start();
		}
	}

	public ArrayList<int[][]> getThreadResult()
	{

		return subResults;
	}

	public ArrayList<int[]> getThreadResult2()
	{

		return subResults2;
	}

	public ArrayList<int[][]> getThreadResult3()
	{

		return subResults3;
	}
}

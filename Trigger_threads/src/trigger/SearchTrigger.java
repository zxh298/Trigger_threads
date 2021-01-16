package trigger;

import java.util.ArrayList;
import java.util.List;

public class SearchTrigger
{
	private int observedVariableNum;

	private ArrayList<int[][]> observedDAGList;

	private ArrayList<int[]> allObservedDAGsDependencyMaticesValues = new ArrayList<int[]>();

	// to store all correct un-repeated hidden models:
	private ArrayList<int[][]> finalUnrepeatedHiddenModels = new ArrayList<int[][]>();

	// whenever replace an arc between two observed variables, the result model
	// is a potential trigger (not guaranteed to be a real trigger)

	private boolean allow_disconnected_nodes;
	
	protected SearchTrigger(int observedVariableNum, boolean allow_disconnected_nodes) throws InterruptedException {
		this.observedVariableNum = observedVariableNum;
		this.allow_disconnected_nodes = allow_disconnected_nodes;
	}

	// get the number of DAGs (fully observed):
	protected int getObservedDAGSize()
	{
		return observedDAGList.size();
	}

	protected void doSearch() throws InterruptedException
	{
		System.out.println("Step 1: Making all possible DAGs....");
		
		long startTime = System.nanoTime();

		DAGList dl = new DAGList(observedVariableNum);

		dl.makeMatrixList(allow_disconnected_nodes);

		
		// Generate all possible DAGs for the current variable number:
		observedDAGList = dl.getResultList();
		
		long endTime = System.nanoTime();
		float duration_seconds = (float) (endTime - startTime) / 1000000000;
		float duration_minutes = (float) duration_seconds / 60;
		float duration_hours = (float) duration_minutes / 60;
		System.out.println("Step 1 total running time: " + duration_hours + " hours");
		System.out.println();
		
		System.out.println("There are " + observedDAGList.size() + " DAGs of " + observedVariableNum + " variables.");
		System.out.println();

		System.out.println("Step 2: Generate dependency matirces for each observed DAG...");
		
		long startTime_step2 = System.nanoTime();
		
		ArrayList<int[]> conditionVarList = new ArrayList<int[]>();
		ArrayList<int[]> remainingVarList  = new ArrayList<int[]>();
		conditionVarList = getConditionVarList(false);
		remainingVarList = getRemainingVarList(false);
		
		if (observedVariableNum < 5)
			
			for (int[][] currentObservedDAG : observedDAGList)
			{
				GetFullLablesCombinationList gc = new GetFullLablesCombinationList(currentObservedDAG, false);

				ArrayList<int[][]> allObservedLabelsCombinationDAGList = gc.getLablesCombinationDAGList();

				for (int[][] currentDAG : allObservedLabelsCombinationDAGList)
				{
					GenerateAllDependencies gd = new GenerateAllDependencies(currentDAG, observedVariableNum, conditionVarList, remainingVarList, false);
					ArrayList<int[][]> dependencyMatricesOfCurrentObservedDAG = gd.getResult();

					// add in the matrix into the dictionary:
					ComputeMatrixValue cm = new ComputeMatrixValue(dependencyMatricesOfCurrentObservedDAG);
					int[] dependencyMatricesValuesOfCurrentObservedDAG = cm.getMatrixValues();
					allObservedDAGsDependencyMaticesValues.add(dependencyMatricesValuesOfCurrentObservedDAG);
				}
			}

		else
		{		
			// divide the tasks into four threads:
			int bit = observedDAGList.size() % 4;
			int boarder = (observedDAGList.size() - bit) / 4;

			ArrayList<int[][]> sub_input1 = new ArrayList<int[][]>();
			ArrayList<int[][]> sub_input2 = new ArrayList<int[][]>();
			ArrayList<int[][]> sub_input3 = new ArrayList<int[][]>();
			ArrayList<int[][]> sub_input4 = new ArrayList<int[][]>();

			for (int i = 0; i < boarder; i++)
			{
				sub_input1.add(observedDAGList.get(i));
				sub_input2.add(observedDAGList.get(i + boarder));
				sub_input3.add(observedDAGList.get(i + boarder * 2));
				sub_input4.add(observedDAGList.get(i + boarder * 3));
			}

			// thread 4 may be longer:
			if (bit > 0)
			{
				for (int i = 0; i < bit; i++)
				{
					sub_input4.add(observedDAGList.get(i + boarder * 4));
				}
			}

			List<Thread> threadList = new ArrayList<Thread>();

			TriggerThread tt1 = new TriggerThread(sub_input1, "Thread-00", conditionVarList, remainingVarList);
			Thread t1 = new Thread(tt1);
			t1.start();
			threadList.add(t1);

			TriggerThread tt2 = new TriggerThread(sub_input2, "Thread-11", conditionVarList, remainingVarList);
			Thread t2 = new Thread(tt2);
			t2.start();
			threadList.add(t2);

			TriggerThread tt3 = new TriggerThread(sub_input3, "Thread-22", conditionVarList, remainingVarList);
			Thread t3 = new Thread(tt3);
			t3.start();
			threadList.add(t3);

			TriggerThread tt4 = new TriggerThread(sub_input4, "Thread-33", conditionVarList, remainingVarList);
			Thread t4 = new Thread(tt4);
			t4.start();
			threadList.add(t4);

			for (Thread t : threadList)
			{
				// waits for this thread to die
				t.join();
			}

			ArrayList<int[]> subResults1 = tt1.getThreadResult2();
			ArrayList<int[]> subResults2 = tt2.getThreadResult2();
			ArrayList<int[]> subResults3 = tt3.getThreadResult2();
			ArrayList<int[]> subResults4 = tt4.getThreadResult2();

			allObservedDAGsDependencyMaticesValues.addAll(subResults1);
			allObservedDAGsDependencyMaticesValues.addAll(subResults2);
			allObservedDAGsDependencyMaticesValues.addAll(subResults3);
			allObservedDAGsDependencyMaticesValues.addAll(subResults4);
			
			
		}

		long endTime_step2 = System.nanoTime();
		float duration_seconds2 = (float) (endTime_step2 - startTime_step2) / 1000000000;
		float duration_minutes2 = (float) duration_seconds2 / 60;
		float duration_hours2 = (float) duration_minutes2 / 60;
		System.out.println("Step 2 total running time: " + duration_hours2 + " hours");
		System.out.println();
		
		/**
		 * Step 3: generate all possible hidden model by replacing each arc by
		 * hidden common cause, and check such models are triggers or not:
		 */
		System.out.println();
		System.out.println("Step 3: Searching triggers...");
		
		long startTime3 = System.nanoTime();
		
		// divide the tasks into four threads:
		int bit = observedDAGList.size() % 4;
		int boarder = (observedDAGList.size() - bit) / 4;

		ArrayList<int[][]> sub_input111 = new ArrayList<int[][]>();
		ArrayList<int[][]> sub_input222 = new ArrayList<int[][]>();
		ArrayList<int[][]> sub_input333 = new ArrayList<int[][]>();
		ArrayList<int[][]> sub_input444 = new ArrayList<int[][]>();

		for (int i = 0; i < boarder; i++)
		{
			sub_input111.add(observedDAGList.get(i));
			sub_input222.add(observedDAGList.get(i + boarder));
			sub_input333.add(observedDAGList.get(i + boarder * 2));
			sub_input444.add(observedDAGList.get(i + boarder * 3));
		}

		// thread 4 may be longer:
		if (bit > 0)
		{
			for (int i = 0; i < bit; i++)
			{
				sub_input444.add(observedDAGList.get(i + boarder * 4));
			}
		}

		conditionVarList = getConditionVarList(true);
		remainingVarList = getRemainingVarList(true);
		
		List<Thread> threadList2 = new ArrayList<Thread>();

		TriggerThread tt1 = new TriggerThread(sub_input111, "Thread-000", allObservedDAGsDependencyMaticesValues, conditionVarList, remainingVarList);
		Thread ttt1 = new Thread(tt1);
		ttt1.start();
		threadList2.add(ttt1);

		TriggerThread tt2 = new TriggerThread(sub_input222, "Thread-111", allObservedDAGsDependencyMaticesValues, conditionVarList, remainingVarList);
		Thread ttt2 = new Thread(tt2);
		ttt2.start();
		threadList2.add(ttt2);

		TriggerThread tt3 = new TriggerThread(sub_input333, "Thread-222", allObservedDAGsDependencyMaticesValues, conditionVarList, remainingVarList);
		Thread ttt3 = new Thread(tt3);
		ttt3.start();
		threadList2.add(ttt3);

		TriggerThread tt4 = new TriggerThread(sub_input444, "Thread-333", allObservedDAGsDependencyMaticesValues, conditionVarList, remainingVarList);
		Thread ttt4 = new Thread(tt4);
		ttt4.start();
		threadList2.add(ttt4);

		for (Thread t : threadList2)
		{
			// waits for this thread to die
			t.join();
		}

		ArrayList<int[][]> subResults11 = tt1.getThreadResult3();
		ArrayList<int[][]> subResults22 = tt2.getThreadResult3();
		ArrayList<int[][]> subResults33 = tt3.getThreadResult3();
		ArrayList<int[][]> subResults44 = tt4.getThreadResult3();

		ArrayList<int[][]> tempTotalResult = new ArrayList<int[][]>();
		tempTotalResult.addAll(subResults11);
		tempTotalResult.addAll(subResults22);
		tempTotalResult.addAll(subResults33);
		tempTotalResult.addAll(subResults44);

		finalUnrepeatedHiddenModels = removeDuplicateTrigger(tempTotalResult);

		for (int[][] trigger : finalUnrepeatedHiddenModels)
		{
			System.out.println("*********************************");
			System.out.println("Candidate trigger:");

			for (int n = 0; n < trigger.length; n++)
			{
				for (int m = 0; m < trigger[n].length; m++)
				{
					System.out.print(trigger[n][m] + " ");
				}

				System.out.println();
			}

			System.out.println();

			System.out.println("*********************************");
		}

		long endTime3 = System.nanoTime();
		float duration_seconds3 = (float) (endTime3 - startTime3) / 1000000000;
		float duration_minutes3 = (float) duration_seconds3 / 60;
		float duration_hours3 = (float) duration_minutes3 / 60;
		System.out.println("Step 3 total running time: " + duration_hours3 + " hours");

		System.out.println("Total number of observed DAGs: " + observedDAGList.size());
	}

	protected int getFinalTriggerNumber()
	{
		return finalUnrepeatedHiddenModels.size();
	}

	private ArrayList<int[][]> removeDuplicateTrigger(ArrayList<int[][]> hiddenModels)
	{
		ArrayList<int[][]> finalUnrepeatedHiddenModels = new ArrayList<int[][]>();

		int[] firstLabel = new int[observedVariableNum + 1];
		for (int i = 0; i < firstLabel.length; i++)
		{
			firstLabel[i] = i;
		}

		GetFullLablesCombinationList gcObserved = new GetFullLablesCombinationList(firstLabel, observedVariableNum + 1);
		ArrayList<int[]> allLabelCombinations = gcObserved.getLabelsCombinationResult();

		for (int[][] currentHiddenModel : hiddenModels)
		{
			boolean hasSameStructure = false;
			// to check whether the current candidate structure is already found
			// before:
			for (int[][] currentStoredHiddenModel : finalUnrepeatedHiddenModels)
			{
				for (int[] currentLabelCombination : allLabelCombinations)
				{
					int unitSame = 0;
					for (int n = 0; n < currentHiddenModel.length; n++)
					{
						for (int m = 0; m < currentHiddenModel[n].length; m++)
						{
							if (currentStoredHiddenModel[currentLabelCombination[n]][currentLabelCombination[m]] == currentHiddenModel[n][m])
							{
								unitSame++;
							}
						}
					}

					if (unitSame == (observedVariableNum + 1) * (observedVariableNum + 1))
					{
						hasSameStructure = true;
						break;
					}
				}
			}

			// if the current candidate hidden model has a new structure:
			if (hasSameStructure == false)
			{
//				// check whether there are isolated parts
//				Connectivity graph = new Connectivity(currentHiddenModel.length);
//				for(int n = 0; n < currentHiddenModel.length; n++)
//				{
//					for(int m = 0; m < currentHiddenModel.length; m++)
//					{
//					   if(currentHiddenModel[n][m] == 1)
//						   graph.makeEdge(m+1, n+1, 1);
//					}
//				}
//				
//				graph.dfs(1);
//				
//				if(graph.isConnected())
//				{
//					finalUnrepeatedHiddenModels.add(currentHiddenModel);	
//				}
//				else
//				{
//					System.out.println("*********************************");
//					System.out.println("Disconnected candidate hidden model:");
//
//					for(int n = 0; n < currentHiddenModel.length; n++)
//					{
//						for(int m = 0; m < currentHiddenModel[n].length; m++)
//						{
//							System.out.print(currentHiddenModel[n][m] + " ");
//						}
//
//						System.out.println();
//					}
//				
//					System.out.println();
//
//					System.out.println("*********************************");
//				}
				
				finalUnrepeatedHiddenModels.add(currentHiddenModel);
				
//				totalNumberOfTriggersFound++;
                						
				System.out.println("*********************************");
				System.out.println("Candidate hidden model:");

				for(int n = 0; n < currentHiddenModel.length; n++)
				{
					for(int m = 0; m < currentHiddenModel[n].length; m++)
					{
						System.out.print(currentHiddenModel[n][m] + " ");
					}

					System.out.println();
				}
			
				System.out.println();

				System.out.println("*********************************");
			}
		}

		return finalUnrepeatedHiddenModels;

	}

	private ArrayList<int[]> getConditionVarList(boolean containHiddenVar)
	{

		/**
		 * Generate all possible condition variable set (by setting currentVarNum to varNum):
		 * 
		 * For example:
		 * 
		 * For variable A, B, C in a given DAG,
		 * the condition variable set are:
		 * Ø(0), 1, 2, 3, 12, 13, 23, 123
		 * 
		 * If the current DAG contains a hidden variable (the first variable), then in this case,
		 * the condition variable set are:
		 * Ø(0), 2, 3, 4, 23, 24, 24, 234
		 *  
		 * */
//		int newVarNum = observedVariableNum;
//		// because we dont condition on the hidden variable, thus varNum should 
//		// change to varNum - 1:
//		if(containHiddenVar == true)
//		{
//			newVarNum = newVarNum - 1;
//		}

		ArrayList<int[]> resultList = new ArrayList<int[]>();

		int[] iArr = new int[observedVariableNum];

		// Set the empty variable set Ø by 0
		int[] fisrtEmptySet = new int[1];
		fisrtEmptySet[0] = 0;
		resultList.add(fisrtEmptySet);

		for (int tempNum = 1; tempNum < observedVariableNum+1; tempNum++)
		{
			int num=0;
			int pos=0;
			int ppi=0;

			for(;;)
			{
				if(num==observedVariableNum)
				{
					if(pos==1)    
						break;

					pos-=2;
					ppi=iArr[pos];
					num=ppi;
				}

				iArr[pos++]=++num;

				if(pos!=tempNum)
					continue;

				int[] tempArray = new int[pos];

				for(int i=0;i<pos;i++){

					tempArray[i] = iArr[i];
				}

				resultList.add(tempArray);

				//System.out.println();
			}
		}

		if(containHiddenVar == true)
		{
			int num = 0;
			for(int[] currentConditionSet : resultList)
			{
				// because the first one is Ø(0) which should not be changed
				if(num > 0)
				{
					//int[] temp = new int[currentConditionSet.length];
					for(int i = 0; i < currentConditionSet.length; i++)
					{					
						currentConditionSet[i] = currentConditionSet[i] + 1;
					}					
				}
				num++;
			}			
		}

		return resultList;
	}
	
	private ArrayList<int[]> getRemainingVarList(boolean containHiddenVar)
	{
		/**
		 *  Generate the remaining variable list which will be 
		 *  applied D-separation rules.
		 *  
		 *  For example:
		 *  
		 *  for a 4 variable matrix
		 *  
		 *    condition         remaining
		 *        Ø(0)          1, 2, 3, 4          
		 *        1             2, 3, 4
		 *        2             1, 3, 4
		 *        3             1, 2, 4 
		 *        4             2, 3, 4
		 *        1, 2          3, 4
		 *        1, 3          2, 4
		 *        1, 4          2, 3
		 *        2, 3          1, 4
		 *        2, 4          1, 3
		 *        3, 4          1, 2
		 *     1, 2, 3          4
		 *     1, 2, 4          3
		 *     1, 3, 4          2
		 *     2, 3, 4          1  
		 *    1, 2, 3, 4        Ø(0)       
		 * */

		ArrayList<int[]> resultList = new ArrayList<int[]>();

		ArrayList<int[]> conditionOnVarList = getConditionVarList(containHiddenVar);

		int varNum = observedVariableNum;
		if(containHiddenVar == true)
			varNum = varNum+1;
		
		// The first remaining variable list should be the full list:		
		int[] firstResult = new int[varNum];        
		for(int i = 0; i < varNum; i++)
		{
			firstResult[i] = i+1;
		}       
		resultList.add(firstResult);

		int num = 0;

		for(int[] currentConditionOnVarList : conditionOnVarList)
		{

			// cuz the first list from resultList is null(0), so skip it.
			if(num == 0)
			{
				num++;
				continue;
			}

			ArrayList<Integer> fullVarArrayList = new ArrayList<Integer>();

			for(int i = 1; i <= varNum; i++)
			{
				fullVarArrayList.add(i);
			}

			for(int var : currentConditionOnVarList)
			{	
				for(int i = 0; i < fullVarArrayList.size(); i++)
				{
					// remove the variables from fullVarArrayList because they exist 
					// in currentConditionOnVarList
					if(fullVarArrayList.get(i) == var)
						fullVarArrayList.remove(i);
				}
			}

			int[] tempResult = new int[fullVarArrayList.size()];
			// transfer the ArrayList to int[]:
			for(int i = 0; i < tempResult.length; i++)
			{
				tempResult[i] = fullVarArrayList.get(i);
			}

			if(tempResult.length == 0)
			{
				int[] finalResult = new int[1];
				finalResult[0] = 0;
				resultList.add(finalResult);
			}
			else 
				resultList.add(tempResult);

		}

		return resultList;
	}
	
	
}

package trigger;

import java.util.ArrayList;
import java.util.List;

public class GenerateAllDAGs
{

	protected static ArrayList<int[]> tempResult = new ArrayList<int[]>();

	public static ArrayList<int[][]> constructFullMatrixList(ArrayList<int[]> tempResultList, int varNum, boolean allow_disconnected_nodes)
	{

		/**
		 *     STEP ONE:
		 *     
		 *     constructFullMatrix Example: 
		 *     
		 *     number of variables: varNum (the number of variables) = 5
		 *     
		 *     1) one array in tempResultList:  1 1 1 0 1 1
		 *                                        
		 *     2) change the shape     ---->          1 1 1                                     
		 *                                              0 1
		 *                                                1
		 *                                       
		 *     3) construct the remaining lower triangle
		 *        by adding 0          ---->      
		 *                                        0 0 1 1 1
		 *                                        0 0 0 0 1
		 *                                        0 0 0 0 1
		 *                                        0 0 0 0 0
		 *                                        0 0 0 0 0 
		 *                                                       
		 */

		ArrayList<int[][]> resultMatrixList = new ArrayList<int[][]>();

		for (int[] array : tempResultList)
		{
			int[][] resultMatrix = new int[varNum][varNum];

			// initialize the result matrix by setting all unit 0:
			for (int m = 0; m < varNum; m++)
			{
				for (int n = 0; n < varNum; n++)
				{
					resultMatrix[m][n] = 0;
				}
			}

			int startPoint = -1;

			for (int m = 0; m < varNum - 1; m++)
			{
				for (int n = m + 1; n < varNum; n++)
				{
					startPoint++;
					resultMatrix[m][n] = array[startPoint];
				}
			}

			/**
			 * STEP TWO: To check whether there exists any separated variables
			 * in the current DAG:
			 */

			if(allow_disconnected_nodes == false)
			{
				boolean hasSeparatedVar = hasSeparatedVariables(resultMatrix, varNum);
				
				if(hasSeparatedVar == false)
					resultMatrixList.add(resultMatrix);
			}
			else
				resultMatrixList.add(resultMatrix);

		}

		/**
		 *  STEP THREE
		 * 
		 *  Remove duplicate matrix
		 * 
		 *  For example, two adjacency matrices:
		 *  
		 *  A:                B:
		 *  0 0 0 1           0 1 0 1
		 *  0 0 1 1    and    0 0 0 1
		 *  0 0 0 1           0 0 0 1
		 *  0 0 0 0           0 0 0 0 
		 *  
		 *  have same DAG structure, so we only keep one of them.
		 *  
		 *  Here, if one matrix can represent another one by changing the
		 *  order of variable labels, these two matrices have same structure.
		 *  
		 *  For matrix A:
		 *  
		 *  The original variable labels order is:
		 *  
		 *   1 2 3 4                          3 1 2 4
		 *  --------                         --------
		 * 1|0 0 0 1                        3|0 0 0 1
		 * 2|0 0 1 1    we change it to:    1|0 0 1 1  
		 * 3|0 0 0 1                        2|0 0 0 1
		 * 4|0 0 0 0                        4|0 0 0 0
		 *  
		 *  The new variable order is newOrder = {3, 1, 2, 4}
		 *  
		 *  Then matrix A can represent matrix B
		 *  (e.g. matrixA[newOrder[0]][newOrder[1]] == matrixB[0][1]
		 *        matrixA[newOrder[0]][newOrder[3]] == matrixB[0][3]
		 *        matrixA[newOrder[1]][newOrder[3]] == matrixB[1][3]
		 *        matrixA[newOrder[2]][newOrder[3]] == matrixB[2][3]
		 *        == 1 )
		 *  
		 *  First, generate all possible combination of new label orders, 
		 *  if the one matrix can represent another one by using one of 
		 *  the orders, these two matrices have same structure.
		 *  
		 *  
		 * */

		ArrayList<int[][]> duplicatedStructureRemovedMatrixList = removeDuplicatedStructures(resultMatrixList, varNum);

		return duplicatedStructureRemovedMatrixList;
	}

	public static boolean hasSeparatedVariables(int[][] currentDAG, int varNum)
	{
		/**
		 * STEP TWO:
		 * 
		 * Check whether there exist separated variables in the current DAG.
		 * 
		 * For example:
		 * 
		 * for four variables with three arcs
		 * 
		 * A->B<-C D
		 * 
		 * D is not connected to any other variables, so it is separated
		 * 
		 * If the separated variable happens, get rid of current DAG
		 * 
		 */

		boolean hasSeparatedVar = true;
		// for each variable:
		for (int num = 0; num < varNum; num++)
		{
			hasSeparatedVar = true;
			// check the row and column of the current variable in the current
			// matrix
			// to make sure that it is adjacent to at least one other variable:
			for (int n = 0; n < varNum; n++)
			{
				if (currentDAG[num][n] == 1 || currentDAG[n][num] == 1)
				{
					hasSeparatedVar = false;
					break;
				}
			}

			if (hasSeparatedVar == true)
			{// theIndexOfDAGHasSeparatedVar.add(i);
				// System.out.println("Separated index: " + i);
				break;
			}
		}

		return hasSeparatedVar;
	}

	public static ArrayList<int[][]> removeDuplicatedStructures(ArrayList<int[][]> currentMatrixList, int varNum)
	{
		// System.out.println("Duplicated structures:");

		ArrayList<int[][]> duplicatedRemovedMatrixList = new ArrayList<int[][]>();

		int[] firstLabel = new int[varNum];

		/**
		 * Construct the first label list.
		 * 
		 * For example:
		 * 
		 * for three variables: 1, 2 and 3
		 * 
		 * the first one is {0,1,2}
		 * 
		 * then from the second one to the last will be:
		 * 
		 * {0,2,1}, {1,0,2}, {1,2,0}, {2,1,0}, {2,0,1}
		 * 
		 */

		// Collect all index of duplicated structures:
		ArrayList<Integer> duplicatedDAGIndex = new ArrayList<Integer>();

		for (int i = 0; i < varNum; i++)
		{
			firstLabel[i] = i;
		}

		// Get all combinations of the current variable number:
		GetFullLablesCombinationList getCombination = new GetFullLablesCombinationList(firstLabel, varNum);
		ArrayList<int[]> allLabelCombinations = getCombination.getLabelsCombinationResult();

		for (int i = 0; i < currentMatrixList.size(); i++)
		{
			int[][] currentMatrix = currentMatrixList.get(i);

			for (int t = i + 1; t < currentMatrixList.size(); t++)
			{
				// to specify whether the current remaining structure
				// is as same as the current matrix (the default value
				// is false)
				boolean isSame = false;

				// if the current remaining matrix is not one the list of
				// duplicated structure:
				if (!duplicatedDAGIndex.contains(t))
				{
					int[][] currentRemainMatrix = currentMatrixList.get(t);

					// try every label order:
					for (int[] currentLabels : allLabelCombinations)
					{
						// the number of the common values in current matrix and
						// current remaining matrix:
						int unitsSameNum = 0;

						for (int n = 0; n < currentRemainMatrix.length; n++)
						{
							for (int m = 0; m < currentRemainMatrix[n].length; m++)
							{
								// to check whether every unit is same between
								// current
								// matrix (using new label order) and current
								// remaining
								// matrix:
								if (currentMatrix[currentLabels[n]][currentLabels[m]] == currentRemainMatrix[n][m])
								{
									unitsSameNum++;
								}
							}
						}

						// if all the units are same:
						if (unitsSameNum == varNum * varNum)
						{
							isSame = true;
							// System.out.println(i + " and " + t);
							break;
						}
					}

					if (isSame == true)
					{
						// if the current matrix and current remaining matrix
						// have
						// same structure, add the index of the remaining matrix
						// to duplicatedDAGIndex:
						duplicatedDAGIndex.add(t);
					}
				}

			}

			if (varNum > 4)
			{
				double percentage = (double) i / currentMatrixList.size();

				String percentage_string = String.format("%.0f", percentage * 100);

				if (percentage % 5 < 1.0 && !percentage_string.equals("0"))
					System.out.println("Remove all duplicated DAGs finishing " + percentage_string + "%...");

			}
		}

		if (duplicatedDAGIndex.size() == 0)
		{
			System.out.println("No duplicated structures.");
		}

		System.out.println();

		// add every distinct structure to the result list:
		for (int i = 0; i < currentMatrixList.size(); i++)
		{
			if (!duplicatedDAGIndex.contains(i))
				duplicatedRemovedMatrixList.add(currentMatrixList.get(i));
		}

		return duplicatedRemovedMatrixList;
	}

	public static ArrayList<int[][]> getAllDAGs(int maxZeroNum, int maximumNumOfArc, int varNum, boolean allow_disconnected_nodes)
			throws InterruptedException
	{
		ArrayList<int[][]> results = new ArrayList<int[][]>();

		int[] array = new int[maximumNumOfArc];

		// initialize the original matrix by setting all units 1:
		for (int zeroNum = 0; zeroNum <= maxZeroNum; zeroNum++)
		{
			for (int i = 0; i < maximumNumOfArc; i++)
			{
				if (i < zeroNum)
					array[i] = 0;
				else
					array[i] = 1;
			}

			/**
			 * For every array with different number of zero, generate the full
			 * combinations:
			 * 
			 * For example:
			 * 
			 * For three variables with one zero: 0, 1, 1
			 * 
			 * all combinations are: 011, 101, 110
			 * 
			 */

			GetFullLablesCombinationList gc = new GetFullLablesCombinationList(array, maximumNumOfArc);
			ArrayList<int[]> gcList = gc.getLabelsCombinationResult();

			for (int[] currentList : gcList)
			{
				tempResult.add(currentList);
			}
		}

		if (varNum <= 4)
			results = constructFullMatrixList(tempResult, varNum, allow_disconnected_nodes);

		if (varNum > 4)
		{
			long startTime = System.nanoTime();
			// divide the tasks into four threads:
			int bit = tempResult.size() % 4;
			int boarder = (tempResult.size() - bit) / 4;

			ArrayList<int[]> sub_input1 = new ArrayList<int[]>();
			ArrayList<int[]> sub_input2 = new ArrayList<int[]>();
			ArrayList<int[]> sub_input3 = new ArrayList<int[]>();
			ArrayList<int[]> sub_input4 = new ArrayList<int[]>();

			for (int i = 0; i < boarder; i++)
			{
				sub_input1.add(tempResult.get(i));
				sub_input2.add(tempResult.get(i + boarder));
				sub_input3.add(tempResult.get(i + boarder * 2));
				sub_input4.add(tempResult.get(i + boarder * 3));
			}

			// thread 4 may be longer:
			if (bit > 0)
			{
				for (int i = 0; i < bit; i++)
				{
					sub_input4.add(tempResult.get(i + boarder * 4));
				}
			}

			List<Thread> threadList = new ArrayList<Thread>();

			TriggerThread tt1 = new TriggerThread(sub_input1, varNum, "Thread-1", allow_disconnected_nodes);
			Thread t1 = new Thread(tt1);
			t1.start();
			threadList.add(t1);

			TriggerThread tt2 = new TriggerThread(sub_input2, varNum, "Thread-2", allow_disconnected_nodes);
			Thread t2 = new Thread(tt2);
			t2.start();
			threadList.add(t2);

			TriggerThread tt3 = new TriggerThread(sub_input3, varNum, "Thread-3", allow_disconnected_nodes);
			Thread t3 = new Thread(tt3);
			t3.start();
			threadList.add(t3);

			TriggerThread tt4 = new TriggerThread(sub_input4, varNum, "Thread-4", allow_disconnected_nodes);
			Thread t4 = new Thread(tt4);
			t4.start();
			threadList.add(t4);

			for (Thread t : threadList)
			{
				// waits for this thread to die
				t.join();
			}

			ArrayList<int[][]> subResults1 = tt1.getThreadResult();
			ArrayList<int[][]> subResults2 = tt2.getThreadResult();
			ArrayList<int[][]> subResults3 = tt3.getThreadResult();
			ArrayList<int[][]> subResults4 = tt4.getThreadResult();

			ArrayList<int[][]> total_result = new ArrayList<int[][]>(subResults1);
			total_result.addAll(subResults2);
			total_result.addAll(subResults3);
			total_result.addAll(subResults4);

			results = removeDuplicatedStructures(total_result, varNum);
			
			long endTime = System.nanoTime();
			float duration_seconds = (float) (endTime - startTime) / 1000000000;
			float duration_minutes = (float) duration_seconds / 60;
			float duration_hours = (float) duration_minutes / 60;
			
			System.out.println("Step 1 total running time: " + duration_hours + " hours.");
		}

		return results;
	}

	public static void main(String[] args) throws InterruptedException
	{

		// int[] array = { 1, 1, 1, 1, 1, 1, 1, 1, 0, 0};
		// allRank(array, 0, 10);
		//
		// ArrayList<int[][]> results = constructFullMatrix(tempResult, 5);

		// getAllDAGs(int maxZeroNum, int maximumNumOfArc, int varNum)
		ArrayList<int[][]> results = getAllDAGs(6, 10, 5, false);

		for (int[][] result : results)
		{
			for (int m = 0; m < 5; m++)
			{
				for (int n = 0; n < 5; n++)
				{
					System.out.print(result[m][n] + " ");
				}
				System.out.println();
			}
			System.out.println("***********************");
		}

		System.out.println("result size: " + results.size());

		// ArrayList<Integer> aa = new ArrayList<Integer>();
		// aa.add(1);
		// System.out.println("test size:" + aa.size());

	}

}

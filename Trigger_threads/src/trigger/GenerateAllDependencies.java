package trigger;

import java.util.ArrayList;

public class GenerateAllDependencies {

	int[][] currentDAG;

	int varNum;
	// to specify whether the current DAG contains a hidden variable (the first variable in the DAG)
	boolean containHiddenVar;
	private ArrayList<int[]> conditionVarList;
	private ArrayList<int[]> remainingVarList;

	public GenerateAllDependencies(int[][]DAG, int varNum, 
			ArrayList<int[]> conditionVarList, ArrayList<int[]> remainingVarList, boolean containHiddenVar)
	{
		this.varNum = varNum;

		currentDAG = new int[varNum][varNum];

		for(int n = 0; n < varNum; n++)
		{
			for(int m = 0; m < varNum; m++)
			{
				currentDAG[n][m] = DAG[n][m];
			}
		}

		this.containHiddenVar = containHiddenVar;
		this.conditionVarList = conditionVarList;
		this.remainingVarList = remainingVarList;
	}


	private ArrayList<int[]> getTwoVarDSeparateCombinationList(int[] currentRemainList)
	{
		/**
		 *  Get all combination of two variables that will be applied 
		 *  D-Separation rules to check their dependencies. 
		 *  
		 *  For example:
		 *  If the current remaining variables are 1, 2, 3
		 *  So all the combinations are: 
		 *  
		 *  1,2    1,3     2,3
		 * */

		ArrayList<int[]> result = new ArrayList<int[]>();

		for(int n = 0; n < currentRemainList.length; n++)
		{			
			for(int m = n+1; m < currentRemainList.length; m++)
			{
				int[] temp = new int[2]; 
				temp[0] = currentRemainList[n];
				temp[1] = currentRemainList[m];

				int num1 = currentRemainList[n];
				int num2 = currentRemainList[m];

				result.add(temp);
			}						
		}

		return result;
	}

	private boolean collisionBlocksThePath(int currentCollision, int[] currentConditionOnVars, ArrayList<Integer> currentPath)
	{
		/**
		 *  0: independent    1: dependent
		 *  
		 *  Check whether there exist collisions in the current path:
		 * 
		 *  1) For the current collision, the nodes before it and after it on the current path
		 *     should be its parents, or the current collision should not be treat as a collision
		 *     of the current path. 
		 *     
		 *     For example:
		 *     If the current path from A to E is A-B-C-D-E, and C is a collision of the current DAG,
		 *     but at least one of B and D is not a parent of C, so C cannot be treat as a collision 
		 *     on this path.
		 *     
		 *  2) If the collisions or their descendants are contained by the current   
		 *     condition variables, the current path is not being blocked (dependent).  
		 *     
		 *  3) If both collisions and their descendants are not contained from the     
		 *     current condition variables, the current path is being blocked.
		 * */

		boolean block = true;

		ArrayList<Integer> currentCollisionParents = new GetCollisions(currentDAG,varNum).getCurrentCollisionParents(currentCollision);
		ArrayList<Integer> currentCollisionDescendants = new GetCollisions(currentDAG,varNum).getCurrentCollisionDescendants(currentCollision);

		int beforeCollisionNodeOnPath = currentPath.get(currentPath.indexOf(currentCollision)-1); 
		int afterCollisionNodeOnPath = currentPath.get(currentPath.indexOf(currentCollision)+1);

		if(currentCollisionParents.contains(beforeCollisionNodeOnPath) && currentCollisionParents.contains(afterCollisionNodeOnPath))
		{
			for(int currentConditionVar : currentConditionOnVars)
			{
				// if the current collision itself is on condition, then block is false: 
				if(currentCollision == currentConditionVar)
				{
					block = false;
					break;
				}

				// if any of the descendant of current collision is on condition, then block is false:
				for(int currentDescendant : currentCollisionDescendants)
				{
					if(currentDescendant == currentConditionVar)
					{
						block = false;
						break;
					}
				}
			}
		}

		else
		{
			// treat the current collision as a normal node (a node in a chain)
			for(int currentConditionVar : currentConditionOnVars)
			{
				if(currentCollision == currentConditionVar)
				{
					block = true;
					break;
				}
				else
				{
					block = false;
				}
			}
		}


		return block;
	}

	public ArrayList<int[][]> getResult()
	{
		/**
		 *  Generate all dependencies between every two variables
		 *  by applying D-separation rules:
		 *  1. chain   2. common cause   3. common effect 
		 *    
		 *  For example, in the following matrix (0:independent 1:dependent):
		 *  
		 *    1 2 3 4
		 * ---------
		 * 1| 0 1 0 0 
		 * 2| 0 0 1 0
		 * 3| 0 0 0 0
		 * 4| 0 0 0 0
		 *  
		 * 1 and 2, 2 and 3 are dependent on each other  
		 *  
		 * One DAG has many condition variable set with its corresponding remaining variable set;
		 * One condition variable set has one dependency matrix;
		 * So each DAG has many dependency matrices. 
		 * 
		 * */

		// the result is a set of two dimensional dependency matrix for a single current DAG
		ArrayList<int[][]> resultDependenciesForCurrentDAG = new ArrayList<int[][]>();
		// get the variable list that being conditioned on: 
//		ArrayList<int[]> conditionOnVarList = getConditionVarList();
		// get the remaining variables except the ones from conditionalVarList:
//		ArrayList<int[]> remainVarList = getRemainingVarList();
		// get the collisions of the current DAG
		ArrayList<Integer> allCollisions = new GetCollisions(currentDAG,varNum).returnCollisions();

		for(int i = 0; i < remainingVarList.size(); i++)
		{
			// get every set of two remaining variables from remainVarList
			int[] currentRemainVariables = remainingVarList.get(i);

			// the variables number of the current remaining set should be bigger than one,
			// if the current model contains a hidden variable, then should be bigger than 
			// three otherwise the remaining variables' number will be bigger than the corresponding
			// observed model.
			if(containHiddenVar == false)
			{

				if(currentRemainVariables.length < 2)
					continue;
			}

			if(containHiddenVar == true)
			{
				if(currentRemainVariables.length < 3)
					continue;
			}

			// get every two variables combinations list from the ith set of remained variables, and which will
			// be checked the dependencies by applying D-Separation rules:      	
			ArrayList<int[]> currentTwoVarCombinationsList = getTwoVarDSeparateCombinationList(currentRemainVariables);

			// get the corresponding ith condition variable set from conditionOn
			int[] currentConditionVariables = conditionVarList.get(i);


			// the current result dependency matrix for the current remained variables
			int[][] currentDependencyMatrix = new int[varNum][varNum];
			for(int n = 0; n < varNum; n++)
			{
				for(int m = 0; m < varNum; m++)
				{
					currentDependencyMatrix[n][m] = 0;
				}        		
			}


			// for every set of two variables from the current combination list:
			for(int[] currentTwoVarCombination : currentTwoVarCombinationsList)
			{
				// get every possible path between the current two variables (false: ignoring the arc directions)
				GetAllPaths gp = new GetAllPaths(currentDAG, varNum, currentTwoVarCombination[0], currentTwoVarCombination[1], false);
				ArrayList<ArrayList<Integer>> paths = gp.getResult();

				// check if there is at least one path is not being blocked by current condition variables
				// if there exists at least one path noting being blocked, these two current remaining variables
				// are dependent.
				int blockedNum = 0;

				// for every path:
				for(ArrayList<Integer> path : paths)
				{										
					// to show whether the path is being blocked 
					//boolean beingBlocked = false;
					boolean beingBlockedByCollision = false;
					boolean beingBlockedByNonCollision = false;
					/**
					 * If the path only contains the two remaining variables, set them dependent
					 * 
					 * For example:
					 * if the two remaining variables are A and B, and we try to check whether they 
					 * are dependent (at least there is one path between them is not being blocked) 
					 * or not, and one of the path only contains A and B (A->B), then we set them 
					 * dependent directly.
					 *
					 * */										
					if(path.size() == 2)
						break;

					/**
					 *  If the path's size is bigger than 2 (there are other variables beside the two 
					 *  remaining variables), remove the two remaining variables (the first and last 
					 *  variable) from the path, because we dont need to condition on them.
					 *  
					 * */
					ArrayList<Integer> newPath = (ArrayList<Integer>) path.clone();
					newPath.remove(0);
					newPath.remove(newPath.size()-1);

					for (int currentNode: newPath)
					{
						// if the current node is a collision:
						if(allCollisions.contains(currentNode))
						{
							// the third parameter should be path instead of newPath because the parents of the current
							// collisions could contain the first(START) node or last(END) node of path.
							beingBlockedByCollision = collisionBlocksThePath(currentNode, currentConditionVariables, path);	

							if(beingBlockedByCollision == true)
							{
								blockedNum++;
								break;
							}
						}

						// if the current node is not a collision:
						else
						{
							for(int currentConditionVar : currentConditionVariables)
							{
								if(currentNode == currentConditionVar)
								{
									beingBlockedByNonCollision = true;
									break;
								}
							}

							if (beingBlockedByNonCollision == true)
							{
								blockedNum++;
								break;
							}
						}
					}


				}
				// if the number of blocked paths is smaller than the total number of paths, it means there exists 
				// a certain number of paths are not being blocked by the current condition variables and the two
				// remaining variables are dependent
				if(blockedNum < paths.size())
				{
					// the two variables are dependent on each other(by setting to 1):
					currentDependencyMatrix[currentTwoVarCombination[0]- 1][currentTwoVarCombination[1]- 1] = 1;
					currentDependencyMatrix[currentTwoVarCombination[1]- 1][currentTwoVarCombination[0]- 1] = 1;
				}								
			}

			resultDependenciesForCurrentDAG.add(currentDependencyMatrix);
		}

		return resultDependenciesForCurrentDAG;		
	}

}

package trigger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainClass
{

	/**
	 * This version convert two dimensional dependency matrix into
	 * 
	 * on dimensional array by multiply each matrix value to 2^n
	 *
	 * 6:20pm Dec 14, 2017
	 * 
	 * Author: Xuhui Zhang
	 */

	public static void main(String[] args) throws IOException, InterruptedException
	{
		// This is the main class of the whole project.
		// All results will be printed in console window.

		/**
		 *     variable number      structures number                    structure number                 number of triggers
		 *                          (without disconnected variables)     (with disconnected variables)                                 
		 *           3                          4                                 6                               0
		 *           4                          24                                31                              2
		 *           5                          268                               302                             57
		 *           6                          5667                              5984                            2525
		 * */

		System.out.println("Please specify the number of variables:");

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String str = br.readLine();

		int observedVariableNum = Integer.valueOf(str);

		// whether consider DAG with disconnected nodes.
		boolean allow_disconnected_nodes = true;
		
		if (observedVariableNum < 3)
			System.out.println("The number should be at least 3!");
		else
		{
			long startTime = System.nanoTime();

			SearchTrigger search = new SearchTrigger(observedVariableNum, allow_disconnected_nodes);

			search.doSearch();

			long endTime = System.nanoTime();
			float duration_seconds = (float) (endTime - startTime) / 1000000000;
			float duration_minutes = (float) duration_seconds / 60;
			float duration_hours = (float) duration_minutes / 60;

			System.out.println("*********************");
			// System.out.println("There are " +
			// search.getAllPossibleTriggerNumber() + " possible hidden
			// models");
			System.out.println("There are " + search.getFinalTriggerNumber() + " of triggers found.");
			System.out.println("Total running time: " + duration_hours + " hours");
			System.out.println("*********************");
		}

	}

}

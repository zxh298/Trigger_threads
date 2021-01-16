package trigger;

import java.util.ArrayList;

public class ComputeMatrixValue {

	private ArrayList<int[][]> matrices;
	
	public ComputeMatrixValue(ArrayList<int[][]> matrices)
	{
		this.matrices = matrices;
	}
	
	
	protected int[] getMatrixValues()
	{
		int varNum = matrices.get(0).length;
		
		int[] values = new int[matrices.size()];
		
		for(int[][] matrix : matrices)
		{
			int matrixIndex = matrices.indexOf(matrix);
			
			int result = 0;
			
			int power = 0;
			
			for(int n = 0; n < varNum -1; n++)
			{
				for(int m=n+1; m < varNum; m++)
				{	
					if(matrix[n][m] == 0)
					{
						power++;
						continue;	
					}
					
				    result += (int)matrix[n][m]*Math.pow(2, power);
				    power++;
				}				
			}
			
			values[matrixIndex] = result;
			
			result = 0;
			
			power = 0;
		}
		
		return values;
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

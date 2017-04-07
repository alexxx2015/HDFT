package edu.tum.uc.jvm;

public class MinNumCoins {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int coins[] = {9,6,1,5};
		int m = coins.length;
		int v = 11;
		System.out.println(minCoinsLin(coins,m,v));
	}
	
	public static int minCoinsExp(int[] coins, int m, int v){
		if (v==0)
			return 0;
		
		int res = Integer.MAX_VALUE;		
		for(int i = 0; i<m; i++){
			if(coins[i] <= v){
				int sub_res = minCoinsExp(coins, m, v - coins[i]);
				if(sub_res != Integer.MAX_VALUE && sub_res +1 < res)
					res = sub_res +1;
			}
		}
		
		return res;
	}
	
	public static int minCoinsLin(int[] coins, int m, int v){
		 int[] table = new int[v+1];
		 table[0] = 0;
		 
		 for(int i = 1; i <= v; i++)
			 table[i] = Integer.MAX_VALUE;
		 
		 for(int i = 1; i <= v; i++){
			 for (int j = 0; j < m; j++){
				 if(coins[j] <= i){
					 int sub_res = table[i -coins[j]];
					 if(sub_res != Integer.MAX_VALUE && sub_res +1 < table[i])
						 table[i] = sub_res + 1;
				 }
			 }
		 }
		 return table[v];
	}

}

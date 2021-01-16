# Trigger_threads

This is a multi-threads version of the Trigger program (https://github.com/zxh298/Trigger).

As it is too slow to search trigger of with more than five observed variables, so we modify the original Trigger program by running on
four threads. This multi-threads version is much faster but runs with more CPU usage. The number of threads could depend on the number of the (logical) cores of your CPU, but we fix to four for now.

     variable number      structures number                   structure number                 number of triggers
                          (without disconnected variables)    (with disconnected variables)                                 
          3                          4                                  6                               0
          4                          24                                 31                              2
          5                          268                                302                             57
          6                          5667                               5984                            2525

"Separated" or "Isolated" means one or more variables are disconnected in the structure, For example A is a separated variables in 
the network (A,B,C,D): A B->C<-D

Please see more details in https://github.com/zxh298/Trigger

You must to cite our publication for any purpose:

Zhang, X., Korb, K. B., Nicholson, A. E. and Mascaro, S. (2017). Applying dependency patterns in causal discovery of latent variable models, Artificial Life and Computational Intelligence, Springer International Publishing, Cham, pp. 134–143.

@InProceedings{zhang2017dependency, </br>
   &nbsp; &nbsp; author="Zhang, Xuhui and Korb, Kevin B. and Nicholson, Ann E. and Mascaro, Steven", </br>
   &nbsp; &nbsp; title="Applying Dependency Patterns in Causal Discovery of Latent Variable Models", </br>
   &nbsp; &nbsp; booktitle="Artificial Life and Computational Intelligence", </br>
   &nbsp; &nbsp; year="2017", </br>
   &nbsp; &nbsp; publisher="Springer International Publishing", </br>
   &nbsp; &nbsp; address="Cham", </br>
   &nbsp; &nbsp; pages="134--143", </br>
   &nbsp; &nbsp; isbn="978-3-319-51691-2" </br>
} 



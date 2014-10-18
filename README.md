REEF
====


Used Data Set
=======
UCI Machine Learning Repository
http://archive.ics.uci.edu/ml/datasets/Physicochemical+Properties+of+Protein+Tertiary+Structure

Physicochemical Properties of Protein Tertiary Structure Data Set 

Attribute Information:

RMSD-Size of the residue. 

F1 - Total surface area. 

F2 - Non polar exposed area. 

F3 - Fractional area of exposed non polar residue. 

F4 - Fractional area of exposed non polar part of residue. 

F5 - Molecular mass weighted exposed area. 

F6 - Average deviation from standard exposed area of residue. 

F7 - Euclidian distance. 

F8 - Secondary structure penalty. 

F9 - Spacial Distribution constraints (N,K Value).

USAGE
=======
java -cp "jar file path" edu.snu.cms.reef.tutorial.MatMultREEF 
-input "input path" -timeout "timeout time" -learnRate "Learning rate value" - numParam "Number of parameters in linear regression data" - targetParam "index of predicting attribute from dataset - index starts from 0"

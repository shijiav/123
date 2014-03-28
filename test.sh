echo "Node 0" > test.txt
awk '
	BEGIN { 
		OFS = "\t"
		n1 = -1
		n2 = -1
		n3 = -1
		count1 = 0
		count2 = 0
		count3 = 0
		lost1 = 0
		lost2 = 0
		lost3 = 0
		print "SENT_Neighbor1", "SENT_Neighbor2", "SENT_Neighbor3", "RECEIVED_Neighbor1", "RECEIVED_Neighbor2", "RECEIVED_Neighbor3", "LOST_Neighbor1", "LOST_Neighbor2", "LOST_Neighbor3"
	}

	$0 ~ "Neighbors of Node" {
		n1 = $5
		n2 = $6
		n3 = $7
	}

	$0 ~ "sent the lost application message" {
		if ( $12 == n1 )
			lost1++
		else if ( $12 == n2 )
			lost2++
		else if ( $12 == n3 )
			lost3++
	}

	$0 ~ "received an application message from" {
		if ( $11 == n1 )
			count1++
		else if ( $11 == n2 )
			count2++
		else if ( $11 == n3 )
			count3++
	}

	$0 ~ "TENTATIVE CHECKPOINT INFO" {
		print $9, $10, $11, count1, count2, count3, lost1, lost2, lost3
	}
' output0.txt >> test.txt
echo "Node 1" >> test.txt
awk '
	BEGIN { 
		OFS = "\t"
		n1 = -1
		n2 = -1
		count1 = 0
		count2 = 0
		lost1 = 0
		lost2 = 0
		print "SENT_Neighbor1", "SENT_Neighbor2", "RECEIVED_Neighbor1", "RECEIVED_Neighbor2", "LOST_Neighbor1", "LOST_Neighbor2"
	}
	NR == 6 {
		n1 = $5
		n2 = $6
	}

	$0 ~ "received an application message from" {
		if ( $11 == n1 )
			count1++
		else if ( $11 == n2 )
			count2++
	}

	$0 ~ "sent the lost application message" {
		if ( $12 == n1 )
			lost1++
		else if ( $12 == n2 )
			lost2++
	}
	$0 ~ "TENTATIVE CHECKPOINT INFO" {
		print $9, $10, count1, count2, lost1, lost2
	}

' output1.txt >> test.txt
echo "Node 2" >> test.txt
awk '
	BEGIN { 
		OFS = "\t"
		n1 = -1
		n2 = -1
		n3 = -1
		count1 = 0
		count2 = 0
		count3 = 0
		lost1 = 0
		lost2 = 0
		lost3 = 0
		print "SENT_Neighbor1", "SENT_Neighbor2", "SENT_Neighbor3", "RECEIVED_Neighbor1", "RECEIVED_Neighbor2", "RECEIVED_Neighbor3", "LOST_Neighbor1", "LOST_Neighbor2", "LOST_Neighbor3"
	}
	NR == 6 {
		n1 = $5
		n2 = $6
		n3 = $7
	}

	$0 ~ "sent the lost application message" {
		if ( $12 == n1 )
			lost1++
		else if ( $12 == n2 )
			lost2++
		else if ( $12 == n3 )
			lost3++
	}

	$0 ~ "received an application message from" {
		if ( $11 == n1 )
			count1++
		else if ( $11 == n2 )
			count2++
		else if ( $11 == n3 )
			count3++
	}

	$0 ~ "TENTATIVE CHECKPOINT INFO" {
		print $9, $10, $11, count1, count2, count3, lost1, lost2, lost3
	}

' output2.txt >> test.txt
echo "Node 3" >> test.txt
awk '
	BEGIN { 
		OFS = "\t"
		n1 = -1
		n2 = -1
		count1 = 0
		count2 = 0
		lost1 = 0
		lost2 = 0
		print "SENT_Neighbor1", "SENT_Neighbor2", "RECEIVED_Neighbor1", "RECEIVED_Neighbor2", "LOST_Neighbor1", "LOST_Neighbor2"
	}
	NR == 6 {
		n1 = $5
		n2 = $6
	}

	$0 ~ "sent the lost application message" {
		if ( $12 == n1 )
			lost1++
		else if ( $12 == n2 )
			lost2++
	}

	$0 ~ "received an application message from" {
		if ( $11 == n1 )
			count1++
		else if ( $11 == n2 )
			count2++
	}

	$0 ~ "TENTATIVE CHECKPOINT INFO" {
		print $9, $10, count1, count2, lost1, lost2
	}

' output3.txt >> test.txt
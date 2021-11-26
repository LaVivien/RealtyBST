
import java.util.*;

/**
 * List of properties for sale.
 * The properties are sorted by price.
 */
@SuppressWarnings( "rawtypes" )
public class Realty extends AbstractSet
  // TODO: Extend abstract set class to be a set of listings (no raw types!)
{
	static final long serialVersionUID = 0L;
	
	public static class Listing {
		private final int price;
		private final String address;
		
		public static final int CEILING = 2_000_000_000;
		
		/**
		 * Create a Realty Listing with the given price and location.
		 * @param price a non-negative integer less than {@link #CEILING}
		 * @param address arbitrary string describing the location (cannot be null)
		 * @throws IllegalArgumentException if price is negative
		 */
		public Listing(int price, String address) {
			if (price < 0) throw new IllegalArgumentException("Price cannot be negative");
			if (price >= CEILING) throw new IllegalArgumentException("Price cannot be $2B or more");
			if (address == null) throw new NullPointerException("Address cannot be null");
			this.price = price;
			this.address = address;
		}
		
		public int getPrice() { return price; }
		public String getAddress() { return address; }
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof Listing) {
				Listing l = (Listing)o;
				return price == l.price && address.equals(l.address);
			} else return false;
		}
		
		@Override
		public int hashCode() {
			return price + address.hashCode();
		}
		
		public String toString() {
			return "$" + price + ":" + address;
		}
		
		/**
		 * Convert a string of the format $N:... into a {@link Listing}.
		 * @param s string to use (must not be null)
		 * @return a Listing (never null)
		 * @throws NumberFormatException
		 * If the string doesn't start with a dollar sign, or if there is no colon,
		 * or if the string between the dollar sign and the (first) colon is not a valid
		 * integer
		 * @throws IllegalArgumentException if N is not under the {@link CEILING}.
		 */
		public static Listing fromString(String s) throws NumberFormatException {
			int colon = s.indexOf(':');
			if (colon < 0) throw new NumberFormatException("Can't find end of price");
			if (s.charAt(0) != '$') throw new NumberFormatException("Price must be in US dollars.");
			return new Listing(Integer.parseInt(s.substring(1, colon)),s.substring(colon+1));
		}
	}
	
	private static class Node {
		Listing entry;
		Node left, right;
		// TODO: Add "parent"
		Node parent = null;
		Node (Listing r) { entry = r;}
	}
	
	private Node root;
	private int numListings;
	// TODO: add version for fail-fast semantics
	int version=0;
	
	private static boolean doReport = true;
	
	/**
	 * Used to report an error found when checking the invariant.
	 * @param error string to print to report the exact error found
	 * @return false always
	 */
	private boolean report(String error) {
		if (doReport) System.out.println("Invariant error found: " + error);
		return false;
	}

	private int reportNeg(String error) {
		report(error);
		return -1;
	}
	
	/**
	 * Check that all listings in the subtree are in range.
	 * Report any errors.  If there is an error return a negative number.
	 * (Write "return reportNeg(...);" when detecting a problem.)
	 * Otherwise return the number of nodes in the subtree.
	 * Note that the range should be updated when doing recursive calls.
	 * @param lo all prices in the subtree rooted at r must be greater than this
	 * @param hi all prices in the subtree rooted at r must be less than this
	 * @return number of nodes in the subtree
	 */
	private int checkInRange(Node r, int lo, int hi) { // TODO: update to check parents too
		if (r == null) return 0;
		if (r.entry == null) return reportNeg("null in tree");
		int price = r.entry.getPrice();
		if (price <= lo) {
			return reportNeg("Entry " + r.entry + " less than " + lo);
		}
		if (price >= hi) {
			return reportNeg("Entry " + r.entry + " greater than " + hi);
		}
		if (r !=root && r.parent == null)
			return reportNeg("Entry " + r.entry + " no parent" );
		if (r == r.parent )
			return reportNeg("Entry " + r.entry + " cycle1" );
		if (r.parent != null ) {
			if (r.parent == r.left || r.parent == r.right)
				return reportNeg("Entry " + r.entry + " cycle2" );
			if (r.parent.left != r  &&  r.parent.right != r )
				return reportNeg("Entry " + r.entry + " skip parent" );
			if (r.parent.parent == null && r.parent != root)
				return reportNeg("Entry " + r.entry + " imposter parent" );
		}
		if (!isInTree(r.parent)) {
			return reportNeg("Entry " + r.entry + "imposter  parent" );
		}
		int n1 = checkInRange(r.left,lo,price);
		int n2 = checkInRange(r.right,price,hi);
		if (n1 < 0 || n2 < 0) return -1; // already reported
		return n1 + n2 + 1;
	}

	/**
	 * Check the invariant.  
	 * Returns false if any problem is found.  It uses
	 * {@link #report(String)} to report any problem.
	 * @return whether invariant is currently true.
	 */
	private boolean wellFormed() {
		int n = checkInRange(root,-1,Listing.CEILING);
		if (n < 0) return false; // problem already reported
		if (n != numListings) return report("numListings is " + numListings + " but should be " + n);
		return true;
	}
	
	/**
	 * Create an empty set of listings.
	 */
	public Realty() {
		root = null;
		numListings = 0;
		assert wellFormed() : "invariant false at end of constructor";
	}
	
	
	// Accessors
	
	public int size() {
		assert wellFormed() : "invariant false at start of size()";
		return numListings;
	}
	
	/**
	 * Return the lowest price realty listing.
	 * @return the lowest price realty listing or null if none at all
	 */
	public Listing getMin() {
		assert wellFormed() : "invariant false at start of getMin()";
		if (root == null) return null;
		Node n = root;
		while (n.left != null) n = n.left;
		return n.entry;
	}
	
	private Listing getNextSubtree(Node r, int floor, Listing best) {
		if (r == null) return best;
		if (r.entry.price > floor) return getNextSubtree(r.left,floor,r.entry);
		else return getNextSubtree(r.right,floor,best);
	}
	
	/**
	 * Get the next realty listing MORE than the given price.
	 * @param floor realty entry must be higher than this price.
	 * @return listing with next higher price, or null if no more.
	 */
	public Listing getNext(int floor) {
		assert wellFormed() : "invariant false at start of getNext()";
		return getNextSubtree(root, floor, null); // call recursive helper
	}
	
	// TODO: perhaps other methods/overrides?
	@Override
	public boolean remove(Object o) {
		// TODO Auto-generated method stub
		Listing l = (Listing) o;
		Node de = delete(l);
		if (de!=null)
			return true;
		return false;
	}	
	
	private void placeUnder(Node n, int price, Node lag) {
		if (lag == null) {
			root = n;
		} else if (price >= lag.entry.getPrice()) {
			lag.right = n;
		} else {
			lag.left = n;
		}
		// TODO: update to handle parent pointers
		n.parent = lag;
	}

	/**
	 * Add a new listing to the realty unless some other listing
	 * already is listed at the same price, in which case return false.
	 * @param l listing to add (must not be null)
	 * @return true if the listing was added.
	 * @throws NullPointerException if the listing is null
	 */
	public boolean add(Listing l) {
		assert wellFormed() : "invariant false at start of add()";
		boolean result = false;
		Node n = root;
		Node lag = null;
		while (n != null) {
			int price = n.entry.price;
			if (price == l.price) break;
			lag = n;
			if (l.getPrice() > price) n = n.right;
			else n = n.left;
		}
		if (n == null) {
			n = new Node(l);
			placeUnder(n, l.getPrice(), lag);
			++numListings; version++;
			result = true;
		}
		assert wellFormed() : "invariant false at end of add";
		return result;
	}

	/**
	 * Add all the listings in the array into this Realty from the range [lo,hi).
	 * The elements are added recursively from the middle, so that
	 * if the array was sorted, the tree will be balanced.
	 * All the tree mutations should be done by add.
	 * The array should not include any nulls in it.
	 * Return number of listings actually added; some might not be added
	 * because they duplicate a price in a previously added listing.
	 * @param array source
	 * @param lo lower bound (usually 0)
	 * @param hi upper bound (using array.length), must be >= lo
	 * @return number of entries added
	 */
	public int addAll(Listing[] array, int lo, int hi) {
		if (lo >= hi) return 0;
		int mid = (lo + (hi - lo)/2);
		int n1 = add(array[mid]) ? 1 : 0;
		int n2 = addAll(array,lo,mid);
		int n3 = addAll(array,mid+1,hi);
		return n1 + n2 + n3;
	}
	
	// TODO: other mutating overrides
	//Delete node by key
    public Node delete(Listing key) {
        if (root == null )
        	return null;
         if ( root.entry.price == key.price){ //delete root
        	root = deleteNode(root); //reset the root
        	numListings--; version++;
        	return root;
         }
        Node curr = root;     
        while (true) { 
            if (key.price > curr.entry.price) {
                if (curr.right == null )
                	break;
                else if (curr.right.entry.price == key.price) {
                	Node oldparent = curr.right.parent;
                    curr.right = deleteNode(curr.right);
                    numListings--; version++;
                    if (curr.right !=null)
                    	curr.right.parent = oldparent;
                    break;
                }
                curr = curr.right;
            } else {
                if (curr.left == null)
                	break;
                else if (curr.left.entry.price == key.price) {
                	Node oldparent = curr.right.parent;
                    curr.left = deleteNode(curr.left);
                    numListings--; version++;
                    if (curr.left !=null)
                    	curr.left.parent = oldparent;
                    break;
                }
                curr = curr.left;
            }
        }
        return root;
    }
    
    //Delete node, Time O(h), Space O(1)
    private Node deleteNode(Node node) {
        if (node == null) 
        	return null;
        if (node.right == null) 
        	return node.left;
        Node curr = node.right;
        while (curr.left != null) //find the left-most node
        	curr = curr.left; 
        curr.left = node.left; // put the original left under left most, 
        if (node.left!=null)
        	node.left.parent = curr; // link the new far left part to old far left
        return node.right;
    }
    
	/**
	 * Copy all the listings (in sorted order) into the array
	 * at the given index.  Return the next index for (later) elements.
	 * This is a helper method for {@link #toArray(Listing[])}.
	 * @param array destination of copy
	 * @param r the subtree whose elements should be copied
	 * @param index the index to place the next element
	 * @return the next spot in the array to use after this subtree is done
	 */
	private int copyInto(Listing[] array, Node r, int index) {
		if (r == null) return index;
		index = copyInto(array,r.left,index);
		array[index++] = r.entry;
		return copyInto(array,r.right,index);
	}
	
	/**
	 * Return an array of all the listings in order.
	 * @param array to use unless null or too small
	 * @return array copied into
	 */
	public Listing[] toArray(Listing[] array) {
		assert wellFormed() : "invariant false at the start of toArray()";
		if (array == null || array.length < size()) array = new Listing[numListings];
		copyInto(array,root,0);
		return array;
	}

	// TODO: required iterator() method
	@Override
	public Iterator iterator() {
		// TODO Auto-generated method stub
		return new MyIterator(root);
	}
	
	/**
	 * Return whether this node is in the tree.
	 * This method is intended to be used in the invariant of the iterator.
	 * This method should be called only when the tree is well-formed,
	 * and will be efficient O(lg n) if the tree is balanced.
	 * This method does assume that the node is a well-formed tree!
	 * @param m node to look for, may be null
	 * @return true if the node is null or is in the tree.
	 */
	//search binary tree's all nodes
	private boolean isInTree(Node n) {
		if (n == null)
			return true;
		return containsNodeRec(root, n);
	}
	
	private boolean containsNodeRec(Node current, Node n) {
	    if (current == null) {
	        return false;
	    } 
	    if ( current.equals(n)) {
	        return true;
	    } 
	    return  containsNodeRec(current.left, n) ||containsNodeRec(current.right, n);
	}
	
	/**
	 * Return the next node (if any) after this one in in-order traversal.
	 * This method should be called only when the tree is well-formed,
	 * and will be efficient O(lg n) if the tree is balanced.
	 * @param n node to start from, must not be null, and must be in the tree
	 * @return next node, may be null
	 */
	private Node getNextNode(Node n) {		
		assert wellFormed() : "getNextNode called when tree is bad";		
		assert isInTree(n) : "getNextNode called with bad argument";
		if (n == null)
			return null;
        if (n.right != null) {
            return lefMost(n.right);
        }
        Node p = n.parent;
        while (p != null && n == p.right) {
            n = p;
            p = p.parent;
        }
        return p;	
		//return null; // TODO: use parent pointers to be efficient
	}
	
	private Node lefMost(Node node) {
        Node current = node;
        while (current.left != null) {
            current = current.left;
        }
        return current;
    }
	
	private class MyIterator implements Iterator<Listing> {
		Node currentNode, nextNode;
		int myVersion = version;
		
		/*
		 * Iterator design:
		 * currentNode points to the node that was recently returned by next().
		 * If it is null, then this mean iteration hasn't started, or else that
		 * the node was removed.
		 * nextNode points to the next node to be iterated over
		 * nextNode null means there are no more elements to iterate over
		 * hasCurrent determines whether there is something that can be removed
		 */
		
		private boolean wellFormed() {
			// TODO: check outer invariant			
			int n = checkInRange(root,-1,Listing.CEILING);
			if (n != numListings) 
				return false;
			// TODO: if versions don't match, accept
			if (myVersion != version)
				return true;
			// TODO: check that currentNode and nextNode are in the tree, or are null
			if (!isInTree(currentNode) || !isInTree(nextNode))  
				return false;
			// TODO: if currentNode is not null, then nextNode must be the next one after it
			if (currentNode != null && nextNode != null && getNextNode(currentNode) != nextNode)
				return false;
			if (nextNode ==null && getNextNode(currentNode) != nextNode)
				return false;
			return true;
		}
		
		//iterator with parent		
		protected MyIterator(boolean ignored) {} // don't change: used by invariant checker
		
		public MyIterator(Node root) {
			// TODO: implement constructor
			//if (root == null) return;
			nextNode = root;
			while (nextNode.left != null)
				nextNode = nextNode.left;
			//currentNode = nextNode;
			assert wellFormed() : "invariant broken at end of constructor";
		}
		
		@Override // required
		public boolean hasNext() {
			assert wellFormed() : "invariant broken in hasNext()";
			//TODO: implement hasNext
			return nextNode != null;
			//return false;
		}

		@Override // required
		public Listing next() {
			assert wellFormed() : "invariant broken at start of next()";
			//TODO: implement next
			currentNode = nextNode ;			 
			nextNode = getNextNode(nextNode);			
			assert wellFormed(): "Invariant broken at end of next()";
			return currentNode.entry;
		}

		
		// TODO: override remove
		//   You may be able to reuse a helper method used by remove(Object o)
		public void remove() throws ConcurrentModificationException {
			delete(currentNode.entry);
			myVersion++;
		}
	}
	
	//testing
	public void inOrder(Node root) {
		if (root != null) {
			inOrder(root.left);
			System.out.print(root.entry +" ");
			if (root.parent!=null)
				System.out.println(root.parent.entry);
			else
				System.out.println("null");
			inOrder(root.right);		
		}
	}
	
	public static void main(String[] args) {
		Realty re = new Realty();  
		Listing i1 = new Listing(50, "a");
		Listing i2 = new Listing(12, "b");
		Listing i3 = new Listing(25, "c");
		Listing i4 = new Listing(75, "d");
		Listing i5 = new Listing(37, "e");
		Listing i6 = new Listing(43, "f");
		
		re.add(i1);
		re.add(i2);
		re.add(i3);
		re.add(i4);
		re.add(i5);
		re.add(i6);
		System.out.println("size: "+re.size());
		System.out.println("root:" +re.root.entry);	
		re.inOrder(re.root);
			
		//remove
		Listing key1=i5;
		re.delete(key1);		
		System.out.println("\nafter delete " + key1);
		re.inOrder(re.root);
		System.out.println();
		
		Iterator my = re.iterator();
		Listing key=i6;
		while (my.hasNext()) {
			Listing item = (Listing)my.next();
			if (item.equals(key)) 
				my.remove();
		}		
		System.out.println("\nafter remove " + key);
		re.inOrder(re.root);	
		System.out.println("size: "+re.size());
		System.out.println("root:" + re.root.entry);
		
		System.out.println(re.numListings + " " + re.version);					
	}
}

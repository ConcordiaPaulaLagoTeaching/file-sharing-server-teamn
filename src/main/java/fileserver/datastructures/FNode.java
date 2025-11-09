package fileserver.datastructures;

public class FNode {
  private int blockIndex;
  private int next;

  public FNode(int blockIndex) {
    this.blockIndex = blockIndex;
    this.next = -1;
  }
}

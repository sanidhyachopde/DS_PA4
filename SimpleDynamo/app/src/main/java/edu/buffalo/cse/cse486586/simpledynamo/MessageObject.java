package edu.buffalo.cse.cse486586.simpledynamo;

public class MessageObject
{
    private String emulatorNo;
    private String previousNode;
    private String nextNode;
    private String key;
    private String value;

    public MessageObject(String emulatorNo, String previousNode, String nextNode, String key, String value)
    {
        this.emulatorNo = emulatorNo;
        this.previousNode = previousNode;
        this.nextNode = nextNode;
        this.key = key;
        this.value = value;
    }

    public String getEmulatorNo()
    {
        return this.emulatorNo;
    }

    public void setEmulatorNo(String emulatorNo)
    {
        this.emulatorNo = emulatorNo;
    }

    public String getPreviousNode()
    {
        return this.previousNode;
    }

    public void setPreviousNode(String previousNode)
    {
        this.previousNode = previousNode;
    }

    public String getNextNode()
    {
        return this.nextNode;
    }

    public void setNextNode(String nextNode)
    {
        this.nextNode = nextNode;
    }

    public String getKey()
    {
        return this.key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getValue()
    {
        return this.value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }
}

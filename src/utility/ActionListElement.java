package utility;

public class ActionListElement implements Comparable<ActionListElement>
{
	public String Name;
	public ActionType Type;
	public Boolean IsCorrect;

	public ActionListElement(String name, ActionType type, boolean isCorrect)
	{
		Name = name;
		Type = type;
		IsCorrect = isCorrect;
	}
	
	@Override
	public int compareTo(ActionListElement o) {
		return Name.compareToIgnoreCase(((ActionListElement)o).Name);
	}
	
	public String toString()
	{
		return Name;
	}
}

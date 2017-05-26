package utility;

public class Action
{
	public ActionType Type;
	public String Name;
	public String OriginalName;

	public Action(ActionType aType, String aName)
	{
		Set(aType, aName);
	}

	public Action() {
	}

	public void Set(ActionType aType, String aName, String anOriginalName)
	{
		Type = aType;
		Name = aName;
		OriginalName = anOriginalName;
	}	
	public void Set(ActionType aType, String aName)
	{
		Type = aType;
		Name = aName;
	}	
	public void Set(ActionType aType)
	{
		Type = aType;
	}

	public void MakeCustom()
	{
		if (Type == ActionType.Message)
		{
			Type = ActionType.CustomMessage;

		}
		else if (Type == ActionType.Sequence)
		{
			Type = ActionType.CustomSequence;
		}
		else
		{
			return;
		}

		OriginalName = Name;
		Name = "";			
	}		
	public void CopyOf(Action someAction)
	{
		Set(someAction.Type, someAction.Name, someAction.OriginalName);
	}

}



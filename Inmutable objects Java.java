//*************************************
//Immutable OBJECTS JAVA
//Immutable object is one that could not be modified once assigned!
//SON ÚTILES EN ENTORNOS MULTI-THREAD DONDE UN HILO CAMBIANDO UN OBJETO DE OTRO HILO PUEDE SER UN BUG DIFICIL DE RASTREAR!
// https://dev.to/monknomo/make-an-immutable-object---in-java-480n
//*************************************

//------------- PREVIEW

public class MyImmutable {
	private final String var1;
	public MyImmutable(String var) {
		this.var1 = var;
	}
	public String getVar1() {return var1;}
}

//Lists, arrays, maps, sets and other non-immutable objects can be surprising.
//Bugs caused by a thread changing another thread's object are often subtle and are very, very hard to track down. Immutable objects stop these whole class of problems in their tracks.

//Por ej. una lista private final no la puedo sustituir, pero sus items sí!

public class MyImmutable {
	private final String[] var1;
	public MyImmutable(String[] var) {
		this.var1 = var;
	}
	public String[] getList() {return var1;}
}

//... MyImmutable list = new MyImmutable(new String[]{"a","b","c"});
//then: list.getList()[0] = "d";
//so we've: 'd','b','c'

//------------- POR TIPO

//------------- Primitives

//Primitives are immutable, so you don't have to do anything special.
//Si el constructor recibe un primitive, asignar simplemente this.var = var, siendo this.var declarada final.

//------------- Collections and Arrays

/*
java.util.Collections provides a number of convenience methods that make converting a Collection to an UnmodifiableCollection a snap.
Collections.unmodifiableCollection
Collections.unmodifiableList
Collections.unmodifiableMap
Collections.unmodifiableNavigableMap
*/
...
//I suggest you store the fields as generic Collections (List, rather than ArrayList).. This allows you to use IDE code generation to make the getters, and contains all the input modifiers in one place
//Make the unmodifiable in the constructor, like so:
public class ImmutableShoppingList {
    private final List<String> list;
    private final int a;

    public ImmutableShoppingList(List<String> list, int a) {
    	this.a = a; //es primitivo, no hacer nada especial!
    	
    	//copiamos la lista, así si modifican la ref original, pues no será la guardada dentro de la lista immutable!
    	//si debo recibir pq sí un Array, pues lo transformo
    	//this.list = Collections.unmodifiableList(Arrays.asList(list));
        List<String> tmpListOfHolding = new ArrayList<>();
        tmpListOfHolding.addAll(list);
        this.list = Collections.unmodifiableList(tmpListOfHolding);
    }

    public List<String> getList(){return this.list;} //si mi impl es con collections
    public String[] getList(){return (String[]) list.toArray();} //si mi impl debe retornar array
}

//------------- Objects

//If the sub-objects are also immutable, you don't have to do anything special.
//If they are not, You need to deep clone them, or the original reference can change your supposedly immutable data out from under your feet.
//Often, you end up working with pre-existing mutable objects, either in your codebase, or in libraries. In this case, I like to create an immutable object wrapper class that extends the mutable class. I find a static getInstance(MutableObject obj) method can be helpful, but a constructor ImmutableObject(MutableObject obj) is also a useful thing to have.

//Wrapper ?.. un wrapper no extiende, encapsula al otro simplemente!, si extiendo luego puedo usar métodos setter del MutableObject
public class ImmutableObject extends MutableObject {
    //debo @Override getters y setters para anularlos! (no lo dice el autor!)
	public ImmutableObject(MutableObject obj) {/*...deep clone obj*/
        //como extiende, comienzo a asignar valores:
        this.varA = obj.getA(); this.varB = obj.getB();
    }
	//or
	public static ImmutableObject getInstance(MutableObject obj) {/*...deep clone obj and return immutable version*/}
}

//otra solución mía!
public class ImmutableObject {
    //creo mis propios getters, pero no setters!
    //public int getA() {return inm.getA();}
    private final MutableObject inm;
    public ImmutableObject(MutableObject obj) {/*...deep clone obj*/
        inm = new MutableObject();
        inm.setA(obj.getA()); inm.setB(obj.getB());
    }
    //or
    public static ImmutableObject getInstance(MutableObject obj) {/*...deep clone obj and return immutable version*/}
}

//parece que diciendo wrapper quería decir (el autor), una clase que reciba los atributos de la Mutable y no tenga setters.

//----- WHAT IF WE THEN WANT TO MAKE OUR OBJECT MODIFIABLE ? -----

//empleamos el Builder pattern, si queremos modificar más adelante los atributos (es otra clase):

//The builder pattern creates a temporary object with the same fields as the desired object.
//It has getters and setters for all the fields. It also has a build() method that creates the desired object

//la haremos una clase en un fichero aparte
//o una public static inner class dentro de la immutable

public class ImmutableDogBuilder {
    private String name;
    private int weight;

    public ImmutableDogBuilder(){}

    public ImmutableDog build(){
        return new ImmutableDog(name, weight);
    }

    public ImmutableDogBuilder setName(String name){
        this.name = name;
        return this;//un setter que retorne this es útil para ir concatenando al construir! (como se ve abajo)
    }

    public ImmutableDogBuilder setWeight(int weight){
        this.weight = weight;
        return this;
    }

    public String getName(){
        return this.name;
    }

    public int getWeight(){
        return this.weight;
    }
}

//así la usamos:
ImmutableDog dog = new ImmutableDogBuilder().setName("Rover").setWeight(25).build();


//----------- PREVENT IMMUTABLES FROM BAD DATA:

//I use two approaches to prevent bad data from getting turned into immutable objects.
//My primary approach is a suite of business rules that test for sane, permissible data. The business rules look at builders, and if the builder passes, I deem it ok to create the immutable object.
//My secondary approach is to embed a small amount of business logic in the immutable object. I don't allow required fields to be null, and any nullable field is an Optional.

//-----------------------------------------
/* The main principles to keep in mind are:
- Clone arrays, Collections and Objects internal to your immutable object
- Use builders when you need a mutable object
- Use Optional to indicate nullable fields in your object's api
- Fail fast on bad data - Objects.requireNonNull can help
*/

/* TIPS:
- If you are using Java 10 or later, you can make defensive copies of collections using 
functions like List.copyOf and Map.copyOf... I've been using the immutable collection classes in Google's Guava library to do the same thing for years.

- In Java8+ there are 
Objects.requireNonNull(object);
Optional<T>, un wrapper que funcionará similar al ? usado en C# para decir que una variable acepta null
*/

/**
Un singleton es una clase que sólo permite una instancia de sí misma
controla el acceso al constructor, sólo la clase puede acceder
se usa cuando hay un recurso que no puede tener sino una instancia,
como el controlador de un periférico
*/


// CONCLUSION

- No usar Singleton, usar InversionOfControl CON UN CONTENEDOR TIPO SPRING QUE MANEJE EL CICLO DE VIDA DEL OBJETO Y NO LO HAGA LA CLASE!

Although singleton design pattern is very famous and popular, it has many serious drawbacks.
Fortunately, with time those are becoming more and more widely recognized and original pattern is now often considered more of an anti-pattern.
Having a single instance is still very useful. However, there are better ways to achieve that.
The best solution is to use a container such as Spring to manage lifecycle independently from the class itself.
This way, the separation of concerns is maintained, the class is reusable in different contexts and well testable.


//WITH LAZY INITIALIZATION (parece que las static se inicializan antes de incluso llamar a la clase ?, pero no me sucedió usando static {} block en la clase)

public final class Singleton {
    private static Singleton INSTANCE = null;

    // Private constructor suppresses default constructor
    private final Singleton(){}

    public static Singleton getInstance() {    
	    if (INSTANCE == null) {
            // Sólo se accede a la zona sincronizada cuando la instancia no está creada
            synchronized(Singleton.class) {
                // En la zona sincronizada sería necesario volver a comprobar que no se ha creado la instancia
                if (INSTANCE == null) { 
                    INSTANCE = new Singleton();
                }
            }
        }
        return INSTANCE;
    }

    //El método "clone" es sobrescrito por el siguiente que arroja una excepción:
    @Override public Object clone() throws CloneNotSupportedException {throw new CloneNotSupportedException();}
}

//------------------------------------------------------------------------------

//WITH EAGER INITIALIZATION (pq según las static se inicializarán antes de usar la clase ?)

    //The fastest (no synchronization)
    //The safest (relies on industrial strength class loader safety)
    //The cleanest (least code - double checked locking is ugly and a lot of lines for what it does)

public final class Singleton {
    
    private static final Singleton INSTANCE = new Singleton(); //class-load initialization ensures it will be created before anything
    //or even before with
    static {
        INSTANCE = new Singleton();
    }

    private final Singleton(){}

    public static Singleton getInstance() {    
        return INSTANCE;
    }

    @Override public Object clone() throws CloneNotSupportedException {throw new CloneNotSupportedException();}
}

//------------------------------------------------------------------------------

//WITH LAZY INITIALIZATION / HOLDER-PATTERN

    //defers initialization of the nested classes until they are actually needed.
    //aunque las static se inicializen antes de usarse, si es inner class no lo hará!

    //The fastest (no synchronization)
    //The safest (relies on industrial strength class loader safety)
    //The cleanest (least code - double checked locking is ugly and a lot of lines for what it does)

public final class Singleton {
    
    private final Singleton() {}

    public static Singleton getInstance() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        private static final Singleton instance = new Singleton();
    }

    @Override public Object clone() throws CloneNotSupportedException {throw new CloneNotSupportedException();}
}

//------------------------------------------------------------------------------

    //Existe otra implementación menos conocida, pero con mayores ventajas dentro de Java que es la siguiente:
    //Igual de segura que apoyarnos en class-load initialization

public enum SingletonEnum {
    INSTANCE;
    
    int value;
    
    //y la lógica de la clase aquí dentro del enum

    public int getValue() {
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
}

//Este enfoque es thread-safe y serializable, garantizado por la implementación de enum.

//Ejemplo de uso:
//SingletonEnum singleton = SingletonEnum.INSTANCE;


//------------------------------------------------------------------------------ DESVENTAJAS DEL SINGLETON PATTERN


EL SINGLETON PATTERN TIENE DESVENTAJAS (https://www.vojtechruzicka.com/singleton-pattern-pitfalls/)

- WHEN TESTING:

Y si usamos una interface ?, necesita constructor y no se tiene acceso.
Debemos llamar a getInstance para luego acceder a cualquier método de instancia, y no podemos crear objetos de esta clase. Ni mockear getInstance porque es static.

*** Usamos PowerMock entonces!

*** Si no existiera PowerMock, seguir leyendo!

public class Singleton {
    private static final Singleton instance = new Singleton();
    private final Singleton() {}
    public static Singleton getInstance() { return instance; }
    public void doSomething() {}
}

One of the main disadvantages of singletons is that they make unit testing very hard. They introduce global state to the application. The problem is that you cannot completely isolate classes dependent on singletons. When you are trying to test such a class, you inevitably test the Singleton as well. When unit testing, you want the class to be as loosely coupled with other classes as possible and all the dependencies of the class should be ideally provided externally (either by constructor or setters), so they can be easily mocked. Unfortunately, that is not possible with singletons as they introduce tight coupling and the class retrieves the instance on its own. But it gets even worse. The global state of stateful singletons is preserved between test cases. That has several serious implications:

-Order of the tests now matters
-Tests can have unwanted side effects caused by singleton
-You cannot run multiple tests in parallel
-Multiple invocations of the same test case can result in different results

- DEPENDENCY HIDING:

*** este hace los tests sólo leyendo las dependencias del método que se le pasan por argumento, no hace un recorrido total!

Usually, when a class requires external collaborators, it is immediately obvious from its constructor's or methods's signature. It is clear which dependencies class has and those can be easily provided. What's more - you can easily provide mocks instead when testing. If a class calls a singleton, it is not obvious from constructor or method. It is even worse when that singleton requires some kind of initialization (like by calling some kind of init(...) method with the initial state). In that case, there is no way for someone who is going to use that class to know, that they should initialize the singleton before. It gets even messier when there is a number of dependent singletons, which need to be initialized in a specific order.

- CLASSLOADERS:

*** resuelvo con JNDI y tengo la instancia manejada desde afuera de la app

Singleton is supposed to have no more than one instance. However the classical implementation does not ensure that there is just one instance per JVM, it only ensures that there is one instance per classloader! If you have a simple client app with just one classloader, you don't need to worry about that. The problem is when using multiple classloaders or deploying the app to an application server. App servers usually use a hierarchy of classloaders. Each deployed app then usually has its own isolated classloader. Different apps deployed on the app server or multiple instances of the same app then do not share one singleton, but each has its own.


- DESERIALIZATION: (ya muy rebuscado)

Singleton prohibits the creation of new instances by hiding its constructor(s), so nobody can call the constructor directly. There are, however, additional means of creating new instances of a class. One of them is serialization/deserialization. If the singleton is supposed to be serializable, you need to make sure only one instance is created during deserialization.

You can achieve this by adding readResolve() method, which returns the current instance of the singleton.

protected Object readResolve() { return getInstance(); }

You prevent multiple deserialization attempts of Singleton. However, it is still not completely safe. The problem is that the readResolve() method is called AFTER the object has been deserialized and can alter object being returned or return different object altogether (like in our case). By returning existing instance from getInstance(), the freshly deserialized instance is not referenced by any other objects and thus is immediately eligible for garbage collection. It is, therefore, possible for an attacker to obtain that reference and keep the copy, preventing it from being garbage collected as described in Effective Java - Item 89: For instance control, prefer enum types to readResolve. To prevent this, you need to declare all the fields of the singleton with object references as transient, which leaves you with the ability to persist only primitives. Josh Bloch suggests using single-item enums as Singleton to prevent this, but in my opinion, it is an abuse of enum for a purpose it is not intended to.


- CONCURRENCY:

*** el mismo se responde que si usas lazy initialization uses double check al crear la instancia.
*** y si usas HolderPattern para hacerla lazy también, pero evitando usar synchronized por el costo que tiene.

A popular variant of singleton uses lazily initialized instance instead of creating it right away. This approach is appropriate when creating the instance is very expensive operation and it is uncertain whether the instance would be used at all.

public static Singleton getInstance() {
    if (instance == null) {
        instance = new Singleton();
    }
    return instance;
}

If the implementation above is used, it is possible that multiple instances are created when multiple threads are accessing the singleton's getInstance() method.

The first thread enters the getInstance() method, while instance is still not initialized
The second thread takes over before the first thread could create the singleton instance and creates it
The first thread continues and creates its own instance
A common fix to that is to declare getInstance() method as synchronized, which prevents this issue. The problem is that then lazy initialization saves unnecessary instance creation at startup, but instead every access to the instance is more expensive due to the synchronization cost. It can be a problem if the instance is frequently accessed. But the only case when the method needs to be synchronized is when the getInstance() is called for the first time.

There are two main approaches how to solve that. The first one is to synchronize just the block, where the instance is initialized. Keep in mind, that you cannot use this approach pre-java 1.5 as it used different memory management model. Also be sure to declare instance field as volatile.

public static Singleton getInstance() {
    if (instance == null) {
        synchronized (Singleton.class) {
            if (instance == null) {
                instance = new Singleton();
            }
        }
    }
    return instance;
}

The second approach is to use the Lazy Initialization Holder class pattern, as described in Java Concurrency in Practice - 16.2.3. Safe Initialization Idioms. This approach eliminates the need for synchronization altogether. It is based on JVM's behavior, which defers initialization of the nested classes until they are actually needed.

public class Singleton {
    private Singleton() {}
    public static Singleton getInstance() {
        return SingletonHolder.instance;
    }
    private static class SingletonHolder {
        private static final Singleton instance = new Singleton();
    }
}


- REFLECTION ATTACK:

*** si el tercero no quiere hacer mal funcionar su app, entonces no tiene que jugar con mi librería!

Class clazz = Singleton.class;
Constructor constructor = clazz.getDeclaredConstructor();
constructor.setAccessible(true);

You do not need to worry about this if you use your singleton just in your application. However, when you distribute a module, which will be used by third parties, this can be an issue. Especially if having multiple instances can result in security risks or other unexpected behavior of your module.




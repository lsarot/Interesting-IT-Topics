
CÓDIGO QUE MANEJA N TAREAS Y BLOQUEA HASTA TERMINARLAS TODAS.
Interfaz AsyncCallback con métodos onError y onSuccess
Impl de dicha interfaz que maneja onError de manera genérica
Clase Sincronizador
	lleva una lista de AsyncCallbackWrapper, una impl de AsyncCallback pero que contiene variable isDone
	un método comprobarEstado para revisar si todos en la lista están o no terminados


EN JAVA YA EXISTE UN THREAD POOL QUE PODEMOS DELEGARLE CALLABLES O RUNNABLES Y ESPERAR A QUE TERMINEN DE EJECUTARSE TODOS!!!



public interface AsyncCallback<T> {
	void onError(Throwable caught);
	void onSuccess(T result);
}

//creo impl de AsyncCallback genérica para manejar todos los errores por igual (SI QUIERO!)
//así luego sólo implemento onSuccess
public abstract class ErrorHandledAsyncCallbackImpl implements AsyncCallback {
	public void onError(Throwable caught) {}
	//al declararla abstract no necesita implementar onSuccess!!!
}


//uso de la impl anterior, suponiendo que se la paso a algún método! (a doSomething abajo)
AsyncCallback asyncCB = new ErrorHandledAsyncCallback() {
	public void onSuccess(Object res) { //puede recibir Object o algún objeto si se le declara usando el parámetro <T>
		//ResultadoOperacion es un objeto genérico que contendrá códidoRsta servidor, objeto resultado, etc.
		ResultadoOperacion res = (ResultadoOperacion) res;
		if (res.isOK()) {/*valida que el resultado sea correcto.. suponiendo que retorna 200 OK (pero puede haber error)*/}
		...
	}
}

//XsService es una interface con método doSomething!, que sabe a qué endpoint comunicarse y qué hacer!
//llama a asyncCB.onSuccess(result), devolviendole el resultado esperado, haciendo un cast o convirtiendo un JSON al tipo de objeto simplemente!
MyServicesFactory.getXsService().doSomething(paramA, paramX, asyncCB);


//usando un sincronizador de llamadas!
Sincronizador sincronizador = new Sincronizador(cmdFinCarga);
AsyncCallback newCallback = sincronizador.getSyncCallback(asyncCB);
//el sincronizador tiene una cola de callbacks a llamar antes de liberar el thread (continuar la ejecución normal)




public class Sincronizador
{
	/**
	 * Conjunto de callbacks donde se registarán los callbacks a sincronizar.
	 * 
	 * @gwt.typeArgs <giros.client.ui.core.Sincronizador.AsyncCallbackWrapper>
	 */
	private List callbacks = new ArrayList();

	/**
	 * Comando que se ejecutará una vez que lleguen todas las respuestas
	 */
	private Command comando;
	
	/**
	 * Indica si ya se han añadido todos los callbacks y el sincronizador está
	 * a la espera de que lleguen todas las respuestas. 
	 */
	boolean enEspera = false;

	public Sincronizador(Command comando) //comando a ejecutar al final de todos los callbacks (si quiero!, sino null)
	{
		this.comando = comando;
	}
	
	/**
	 * Registra un {@link AsyncCallback} para sincronizarlo con el panel.
	 * 
	 * @param callback {@link AsyncCallback} a sincronizar
	 * 
	 * @return un {@link AsyncCallback} que realmente es una clase que la
	 * envuelve y delega en el {@link AsyncCallback} original la lógica y 
	 * mantiene un estado que indica si ha terminado o no la invocación
	 * remota
	 */
	public AsyncCallback getSyncCallback(AsyncCallback callback)
	{
		AsyncCallbackWrapper wrapper = new AsyncCallbackWrapper(callback, this);
		callbacks.add(wrapper);
		return wrapper;
	}

	/**
	 * La pantalla se pone en modo espera
	 */
	public void iniciarEspera() 
	{
		enEspera = true;
		
		// Hace una primera comprobación del estado por si acaso
		// hubiesen llegado todas las respuestas mientras se inicializaba.
		comprobarEstado();
	}
	
	/**
	 * Incrementa el número de llamadas realizadas en 1 (de manera de protegida)
	 * y si ha realizado todas las llamas cierra este popup y abre el panel de
	 * busqueda con los datos cargados 
	 */
	protected void comprobarEstado()
	{
		if(enEspera != true) {
			return;
		}
		Iterator it = callbacks.iterator();
		while(it.hasNext())
		{
			AsyncCallbackWrapper callback = (AsyncCallbackWrapper)it.next();
			// Con que quede una sin responder tenemos que seguir a la espera
			if(!callback.isDone()) {
				GWT.log("Sincronizador.comprobarEstado: SI hay respuestas pendientes", null);
				return;
			}
		}
		// Si todas las consultas estaban respondidas damos por concluida la espera.
		// Cerramos la ventana y ejecutamos el comando para informar del fin de la espera.
		GWT.log("Sincronizador.comprobarEstado: NO hay respuestas pendientes", null);
		enEspera = false;
		callbacks.clear();
		callbacks = null;
		if(comando != null) {
			comando.execute();
		}
		comando = null;
	}
	
	/**
	 * Clase que envuelve los {@link AsyncCallback} y que contiene información
	 * de estado (si han vuelto o no han vuelto).  Delega en el 
	 * {@link AsyncCallback} original la implementación de los métodos
	 */
	private class AsyncCallbackWrapper implements AsyncCallback
	{
		/**
		 * Callback original y en el que delegan los métodos de {@link AsyncCallback}
		 */
		private AsyncCallback callback;
		
		/**
		 * Indicador de si la llamada ha terminado
		 */
		private boolean done = false;
		
		/**
		 * Sincronizador asociado con el AsyncCallbackWrapper
		 */
		private Sincronizador sincronizador;
		
		/**
		 * Instancia un {@link AsyncCallbackWrapper} a partir de un 
		 * {@link AsyncCallback} que es el que realmente implementa la lógica de llamada.
		 * 
		 * @param callback
		 */
		private AsyncCallbackWrapper(AsyncCallback callback, Sincronizador sincronizador)
		{
			super();
			this.callback = callback;
			this.sincronizador = sincronizador;
		}

		/* (non-Javadoc)
		 * @see com.google.gwt.user.client.rpc.AsyncCallback#onFailure(java.lang.Throwable)
		 */
		public void onFailure(Throwable caught)
		{
            try {
                callback.onFailure(caught);
            }
            catch(Exception e) { 
                GWT.log(e.toString(), e); 
            };
			done = true;
			sincronizador.comprobarEstado();
		}

		/* (non-Javadoc)
		 * @see com.google.gwt.user.client.rpc.AsyncCallback#onSuccess(java.lang.Object)
		 */
		public void onSuccess(Object result)
		{
            try {
                callback.onSuccess(result);
            }
            catch(Exception e) { 
                GWT.log(e.toString(), e); 
            };
			done = true;
			sincronizador.comprobarEstado();
		}

		public boolean isDone()
		{
			return done;
		}
	}
	


	*** ESTOS MÉTODOS ORANGE PARECEN RELLENO INSERVIBLE ***



	public AsyncCallback getCallback(final ComandoOrange comando)
	{
		AsyncCallback callback = this.getSyncCallback(new AsyncCallbackOrange(comando));
		return callback;
	}
	
	/**
	 * Añade una llamada a la lista de llamadas, pero no añade ningún parámetro
	 * 
	 * @param lista constante de la consulta a realizar
	 * @param combo combo que recibirá los valores
	 */
	public void aniadirLlamada(EnumListados lista, ComandoOrange combo)
	{
		AsyncCallback callback = this.getCallback(combo);
		FactoriaServicios.getComunService().obtenerListado(lista, callback);
	}

	/**
	 * Añade una llamada a la lista de llamadas con 1 parametro
	 * 
	 * @param lista constante de la consulta a realizar
	 * @param combo combo que recibirá los valores
	 * @param parametro parámetro de la consulta 
	 */
	public void aniadirLlamada(EnumListados lista, ComandoOrange combo, String parametro)
	{
		AsyncCallback callback = this.getCallback(combo);
		FactoriaServicios.getComunService().obtenerListado(lista, new QueryParam(parametro), callback);
	}

	/**
	 * Añade una llamada a la lista de llamadas con 2 parametros
	 * 
	 * @param lista constante de la consulta a realizar
	 * @param combo combo que recibirá los valores
	 * @param parametro1 primer parámetro de la consulta 
	 * @param parametro2 segundo parámetro de la consulta 
	 */
	public void aniadirLlamada(EnumListados lista, ComandoOrange combo, String parametro1, String parametro2)
	{
		AsyncCallback callback = this.getCallback(combo);
		FactoriaServicios.getComunService().obtenerListado(lista, new QueryParam(parametro1), new QueryParam(parametro2), callback);
	}


}




package co.edu.rentainmuebles.model;
/** Numero tratado como texto para conservar ceros iniciales. */
public record CuentaBancaria(long bancoId,String numeroCuenta,String tipoCuenta) {
 public CuentaBancaria {
  if(bancoId<=0)throw new IllegalArgumentException("Seleccione un banco.");
  if(numeroCuenta==null||!numeroCuenta.matches("[0-9]{4,34}"))throw new IllegalArgumentException("El numero de cuenta debe contener entre 4 y 34 digitos.");
  if(!java.util.Set.of("AHORROS","CORRIENTE").contains(tipoCuenta==null?"":tipoCuenta))throw new IllegalArgumentException("Seleccione ahorros o corriente.");
 }
 public long getBancoId(){return bancoId;}public String getNumeroCuenta(){return numeroCuenta;}public String getTipoCuenta(){return tipoCuenta;}
}

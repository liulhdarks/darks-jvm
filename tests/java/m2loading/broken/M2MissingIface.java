public interface M2MissingIface
{
    int TAG = 1;

    default String tag()
    {
        return "missing-iface";
    }
}

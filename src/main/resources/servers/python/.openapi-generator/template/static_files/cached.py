import hashlib
from logging import getLogger
import pathlib
import pickle

from connexion.spec import Specification

logger = getLogger(__name__)


class CachedSpecification(Specification):
    """Cache the built API specification.

    Building and loading our OpenAPI specification is very slow, by caching
    the result we can drastically reduce the reload time of the application.
    The cache is invalidated when the yaml file changes.
    """

    @classmethod
    def from_file(cls, spec, arguments=None):
        md5_hash = cls.md5(spec)
        cache_file = str(spec) + '.cache'
        try:
            with open(cache_file, 'rb') as f:
                cache = pickle.load(f)
                if cache['md5_hash'] == md5_hash:
                    logger.info('Loaded spec from cache')
                    return cache['spec']
        except OSError as e:
            logger.warning('Cache file does not exist: %s', e)
            pass

        rv = cls._real_from_file(spec, arguments=arguments)
        try:
            with open(cache_file, 'wb') as f:
                cache = {
                    'md5_hash': md5_hash,
                    'spec': rv
                }
                pickle.dump(cache, f)
                logger.info('Stored spec in cache')
        except OSError as e:
            logger.warning('Could not store spec in cache: %s', e)

        return rv

    @classmethod
    def _real_from_file(cls, spec, arguments=None):
        """
        Takes in a path to a YAML file, and returns a Specification
        """
        specification_path = pathlib.Path(spec)
        spec = cls._load_spec_from_file(arguments, specification_path)
        return cls.from_dict(spec)

    @classmethod
    def md5(cls, file) -> str:
        hash_md5 = hashlib.md5()
        with open(file, 'rb') as f:
            for chunk in iter(lambda: f.read(4096), b""):
                hash_md5.update(chunk)
        return hash_md5.hexdigest()